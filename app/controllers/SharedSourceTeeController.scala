package controllers

import javax.inject._

import akka.stream.scaladsl.Source
import play.api.http.HttpEntity
import play.api.libs.ws.{StreamedBody, StreamedResponse, WSClient}
import play.api.mvc._

import scala.concurrent.ExecutionContext

@Singleton
class SharedSourceTeeController @Inject()(wsClient: WSClient, config: BaseUrl)(implicit ec: ExecutionContext) extends Controller {

  /**
    * ==Goal==
    * We want to stream the WS response body to both:
    * 1. the caller via the [[Result]]
    * 2. the analytics team via some REST endpoint
    *
    * ==Approach==
    * Pass the same [[Source]] instance to both the [[Result]] and the [[StreamedBody]].
    *
    * ==Problem==
    * This doesn't work because the same [[Source]] cannot be shared
    * by both the [[Result]] and the WS [[StreamedBody]].
    *
    * Specifically, `curl localhost:9000` results in:
    * curl: (18) transfer closed with 1270 bytes remaining to read
    *
    * And under unit tests:
    * {{{
    * This publisher only supports one subscriber
    * java.lang.IllegalStateException: This publisher only supports one subscriber
    * 	at com.typesafe.netty.HandlerPublisher.subscribe(HandlerPublisher.java:167)
    * }}}
    */
def tee: Action[AnyContent] = Action.async {
  // We're going to do a GET and feed the response Source to two places.
  wsClient.url(config.baseUrl)
    .stream()
    .map { case StreamedResponse(headers, source) =>
      val simpleHeaders = headers.headers.mapValues(_.head)
      val contentLength = simpleHeaders(CONTENT_LENGTH)
      val contentType = simpleHeaders(CONTENT_TYPE)

      // Stream it as the POST body of another request.
      wsClient.url(s"${config.baseUrl}/log/response/body")
        .withMethod("POST")
        .withHeaders(CONTENT_LENGTH -> contentLength, CONTENT_TYPE -> contentType)
        .withBody(StreamedBody(source)) // (1)
        .execute()

      // Stream it to the caller in the Result.
      Result(
        header = ResponseHeader(
          status = headers.status,
          headers = simpleHeaders.filterKeys(!_.startsWith("Content-")) // shush the NettyModelConversion warning.
        ),
        body = HttpEntity.Streamed(
          data = source, // (2)
          contentLength = Some(contentLength.toLong),
          contentType = Some(contentType)
        )
      )
    }
}
}

@Singleton
case class BaseUrl(baseUrl: String) {

  def this() = this("https://www.example.com")
}
