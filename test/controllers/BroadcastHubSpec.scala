package controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{BroadcastHub, Keep, Sink, Source}
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}

class BroadcastHubSpec
  extends FunSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with TypeCheckedTripleEquals {

  describe("BroadcastHub") {
    implicit val actorSystem = ActorSystem()
    implicit val materializer = ActorMaterializer()

    it("should not have a race condition causing data loss for my consumers") {
      val source = Source(1 to 10)
      val producer = source.toMat(BroadcastHub.sink(bufferSize = 8))(Keep.right).run()

      /**
        * First one works fine (and consumes all the elements).
        */
      producer.runWith(Sink.seq).futureValue should contain theSameElementsInOrderAs (1 to 10)

      /**
        * Second one subscribes after the first has already consumed all the elements.
        *
        * Assertion fails with:
        * {{{
        *   Vector() did not contain the same elements in the same (iterated) order as Range(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        * }}}
        */
      producer.runWith(Sink.seq).futureValue should contain theSameElementsInOrderAs (1 to 10)
    }

    it("should return data if the buffer is larger than the number of source elements") {
      val source = Source(1 to 10)
      val producer = source.toMat(BroadcastHub.sink(bufferSize = 16))(Keep.right).run()

      /**
        * Assertion fails with:
        * {{{
        *   Vector() did not contain the same elements in the same (iterated) order as Range(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        * }}}
        *
        * I have no hypothesis as to why this is the case.
        */
      producer.runWith(Sink.seq).futureValue should contain theSameElementsInOrderAs (1 to 10)
    }
  }
}
