package testkit

import org.scalatest.{BeforeAndAfterAll, Suite}
import play.api.inject.DefaultApplicationLifecycle

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Provides an [[play.api.inject.ApplicationLifecycle]]
  * that can be used to register hooks to be run after all tests complete.
  */
trait WithAfterAllTestsLifecycle extends BeforeAndAfterAll {
  this: Suite =>

  protected implicit val afterAllTestLifecycle: AfterAllTestsLifecycle = new AfterAllTestsLifecycle

  override protected def afterAll(): Unit = {
    Await.result(afterAllTestLifecycle.stop(), 15.seconds)
    super.afterAll()
  }
}

class AfterAllTestsLifecycle extends DefaultApplicationLifecycle
