package controllers

import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfter, LoneElement, OptionValues}
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Action, BodyParsers, RawBuffer, Results}
import play.api.routing.sird
import play.api.routing.sird._
import play.api.test.Helpers._
import play.api.test._
import play.core.server.Server
import testkit.{NonGlobalTestServer, WithAfterAllTestsLifecycle}

import scala.collection.mutable.ArrayBuffer

/**
  * Add your spec here.
  * You can mock out a whole application including requests, plugins etc.
  *
  * For more information, see https://www.playframework.com/documentation/latest/ScalaTestingWithScalaTest
  */
class SharedSourceTeeControllerSpec
  extends PlaySpec
    with GuiceOneAppPerTest
    with BodyParsers
    with Results
    with BeforeAndAfter
    with Eventually
    with LoneElement
    with OptionValues
    with WithAfterAllTestsLifecycle {

  implicit private lazy val materializer = app.materializer

  /**
    * Keep track of POST bodies here.
    * Clearing it [[before]] each test.
    */
  private var serverInvocations: ArrayBuffer[RawBuffer] = _
  before(serverInvocations = ArrayBuffer.empty[RawBuffer])

  /**
    * A server to both provide the GET content and receive the POST body.
    */
  private lazy val upstreamServer: Server = NonGlobalTestServer() {
    case sird.GET(p"/") => Action(Ok("streamy"))
    case sird.POST(p"/log/response/body") => Action(parse.raw) { request =>
      serverInvocations += request.body
      Ok
    }
  }

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .bindings(bind[BaseUrl].to(BaseUrl(s"http://localhost:${upstreamServer.httpPort.get}")))
    .build()

  "SharedSourceTeeController" should {

    "send content in both the Result and the WS POST request" in {
      val controller = app.injector.instanceOf(classOf[SharedSourceTeeController])

      val result = controller.tee().apply(FakeRequest())

      status(result) mustBe OK
      contentAsString(result) must include("streamy")
      eventually {
        new String(serverInvocations.loneElement.asBytes().value.toArray) mustBe "streamy"
      }
    }
  }
}
