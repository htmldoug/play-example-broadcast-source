package testkit

import play.api._
import play.api.inject.ApplicationLifecycle
import play.api.mvc.{Handler, RequestHeader}
import play.api.routing.Router
import play.core.server.{Server, ServerConfig, ServerProvider}

/**
  * A Play server that you can run multiple copies of.
  *
  * Most notably, it avoids calling [[Play.start()]] which
  * otherwise registers a server as the ONE TRUE PLAY SERVER,
  * stopping the currently running ONE TRUE PLAY SERVER.
  */
object NonGlobalTestServer {

  /**
    * Mirrors [[Server.withRouter()]], with a few differences.
    *
    * This does not call [[Play.start()]] to avoid taking over as the global play server.
    * Makes it possible to spin up multiple servers.
    *
    * Does not call [[Application.stop()]]. That's up to the caller.
    *
    * This requires an [[ApplicationLifecycle]] to register stop hooks.
    */
  def apply(
    config: ServerConfig = ServerConfig(port = Some(0), mode = Mode.Test)
  )(routes: PartialFunction[RequestHeader, Handler])(
    implicit
    provider: ServerProvider,
    lifecycle: ApplicationLifecycle
  ): Server = {

    /**
      * This all is mostly lifted from [[Server.withRouter()]] and [[Server.withApplication()]].
      */
    val app: Application = new BuiltInComponentsFromContext(TestAppContext(config)) {
      def router: Router = Router.from(routes)
    }.application

    lifecycle.addStopHook(() => app.stop())

    provider.createServer(config, app)
  }
}
