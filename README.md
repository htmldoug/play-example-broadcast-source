## Goal
For a controller:
```scala
GET     /          controllers.SharedSourceTeeController.tee
```

...that makes a call using WS client:
```angular2html
wsClient.url("...").stream()
```
...I'd like to stream the same response body through both:
1. the play action `Result(..., Streamed(source))` 
2. our analytics team's REST server via another WS POST request `wsClient.url("...").withBody(StreamedBody(source))`

## Problem
I can't pass the same Source to multiple publishers.

```scala
java.lang.IllegalStateException: This publisher only supports one subscriber
	at com.typesafe.netty.HandlerPublisher.subscribe(HandlerPublisher.java:167)
	at akka.stream.impl.MaterializerSession.akka$stream$impl$MaterializerSession$$doSubscribe(StreamLayout.scala:1033)
	at akka.stream.impl.MaterializerSession.assignPort(StreamLayout.scala:1025)
	at akka.stream.impl.MaterializerSession$$anonfun$exitScope$2.apply(StreamLayout.scala:907)
	at akka.stream.impl.MaterializerSession$$anonfun$exitScope$2.apply(StreamLayout.scala:906)
	at scala.collection.Iterator$class.foreach(Iterator.scala:893)
	at scala.collection.AbstractIterator.foreach(Iterator.scala:1336)
	at akka.stream.impl.MaterializerSession.exitScope(StreamLayout.scala:906)
	at akka.stream.impl.MaterializerSession$$anonfun$materializeModule$1.apply(StreamLayout.scala:958)
	at akka.stream.impl.MaterializerSession$$anonfun$materializeModule$1.apply(StreamLayout.scala:950)
	at scala.collection.immutable.Set$Set2.foreach(Set.scala:128)
	at akka.stream.impl.MaterializerSession.materializeModule(StreamLayout.scala:950)
	at akka.stream.impl.MaterializerSession.materialize(StreamLayout.scala:917)
	at akka.stream.impl.ActorMaterializerImpl.materialize(ActorMaterializerImpl.scala:256)
	at akka.stream.impl.ActorMaterializerImpl.materialize(ActorMaterializerImpl.scala:146)
	at akka.stream.scaladsl.RunnableGraph.run(Flow.scala:350)
	at akka.stream.scaladsl.Source.runWith(Source.scala:81)
	at akka.stream.scaladsl.Source.runFold(Source.scala:91)
	at play.api.http.HttpEntity$class.consumeData(HttpEntity.scala:48)
	at play.api.http.HttpEntity$Streamed.consumeData(HttpEntity.scala:94)
	at play.api.test.ResultExtractors$class.contentAsBytes(Helpers.scala:331)
	at play.api.test.Helpers$.contentAsBytes(Helpers.scala:382)
	at play.api.test.ResultExtractors$class.contentAsString(Helpers.scala:324)
	at play.api.test.Helpers$.contentAsString(Helpers.scala:382)
	at controllers.SharedSourceTeeControllerSpec$$anonfun$2$$anonfun$apply$mcV$sp$1.apply(SharedSourceTeeControllerSpec.scala:69)
	at controllers.SharedSourceTeeControllerSpec$$anonfun$2$$anonfun$apply$mcV$sp$1.apply(SharedSourceTeeControllerSpec.scala:63)
```

I don't see an obvious way to return multiple Sources via the GraphDSL.

Using something like `Sink.asPublisher(fanout = true)` seems like it should work, but introduces a race condition. 
```scala
val publisher = source.runWith(Sink.asPublisher(fanout = true).withAttributes(Attributes.inputBuffer(1, 100)))
val source1 = Source.fromPublisher(publisher)
val source2 = Source.fromPublisher(publisher)
```
If the consumer of `source2` starts consuming from the source after `source1`, it gets only a subset of the messages.
