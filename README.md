# Kafka Actors for Lift

_This project is still a work in progress, and its API is still rapidly evolving._

This project implements Kafka Actors for Lift, a solution that allows Lift Actors to talk to
one another via Kafka Brokers. This has value for distributed and non-distributed systems. It
enables use cases such as:

* **Durable mailboxes.** If a standard `LiftActor` were to crash during processing or the process
  running it got `kill -9`, the contents of its mailbox would be lost. `KafkaActor`s only commit
  offsets to Kafka once they've finished processing the messages in a batch. Restarting the actors
  in the middle of a batch will cause it to pick up from its last checkpoint and re-process
  messages. This ensures at-least-once processing.
* **Distributed communication.** Many times when an actor needs to reach out to another system
  in your infrastructure, it'll need to resort to some synchronous operation (e.g. REST). Further,
  handling failures in the destination service typically needs to be hand-rolled unless you bring in
  an RPC library like finagle. Using Kafka as a simple message brokers eliminates a lot of that
  pain. Further, Kafka is easier to maintain, secure, replicate, and administrate than many other
  message broker solutions.
* **Load sharing.** If you have actors doing heavy lifting in your system, you may wish to run
  many processes connected to a multi-partition topic to spread the load around your infrastructure.
  `KafkaActor`s that connect to the same topic with the same group ID will have different
  partitions assigned to them by Kafka. This effectively means you can send requests to "the cluster
  of all actors currently connected to Kafka" and hard things like failover are handled for you.

This project is still in early, experimental development. However, we believe it could be useful
to anyone who would like to pair Lift Actors with a Kafka broker backend.

## Adding to your project

To add this to your project, you'll want to ensure that you include `lift-json` and `lift-actor`.
Both are required for this to work correctly. (If you have `lift-webkit` you already have them.)
Further, like the rest of Lift, this requires Java 8.

This library is built against Lift 3.2 and 3.3, and should work with any
Lift build with these versions.

To use this project add it to your library dependencies:

```scala
libraryDependencies += "net.liftmodules" %% "kafka-actors_3.3" % "0.2.0"
```

If you're using Lift 3.2, change the suffix on the artifact name above to
`_3.2` instead.

If you don't already use lift-webkit, then make sure you have lift-json and lift-actor as well:

```scala
libraryDependencies += "net.liftweb" %% "lift-actor" % "3.3.0"
libraryDependencies += "net.liftweb" %% "lift-json" % "3.3.0"
```

## Using Kafka Actors

Using Kafka Actors imposes a few restrictions on the messages you can send to your actors.

Specifically:

* Your messages must be serializable and deserializable with lift-json
* Generally the above means that you can't get fancy with generic types

If these constraints don't cramp your style, then Kafka Actors may be for you. To get started
you'll need to declare the actual implementation of your actor. Consider the following example
from our example code in this repo:

```scala
import net.liftmodules.kafkaactors._
import net.liftweb.common._

case class Ping()

object ExampleConsumer extends KafkaActor with Loggable {
  override val bootstrapServers = "localhost:9092"
  override val kafkaTopic = "kafka-actors-example-consumer"
  override val groupId = "kafka-actors-example-consumer"
  override val pollTime = 1000

  override def userMessageHandler = {
    case Ping() =>
      logger.info("Got ping!")
  }
}
```

This actor will connect to a Kafka broker on the local machine and consume messages from the
`kafka-actors-example-consumer` Kafka topic. It will use the group id
`kafka-actors-example-consumer` and it will wait 1 second for new messages in its event loop before
giving up, doing some housecleaning, and polling again.

When this actor receives the `Ping` message, it will log that it received the message.

Unlike a standard `LiftActor`, this actor will only accept messages with the marker trait
`KafkaActorMessage` attached. We do this to ensure that we have enough type information to
serialize and deserialize the object. This is a useful signal to developers to be careful
about what they put in the message objects.

At this point, the actor will function much like a regular actor. We can send it messages from the
local process the same way we always would:

```scala
ExampleConsumer ! Ping()
```

This will work just fine and bypass Kafka entirely. However, part of the fun of a `KafkaActor` is
to use Kafka, right? To signal that the actor should connect up to Kafka, just do the following:

```scala
ExampleConsumer ! StartConsumer
```

This will start up the Kafka consumer thread in the background and cause the actor to start reading
from the specified Kafka topic. (There's also a `StopConsumer` parallel that will cleanly shut down.)

Now how do we send messages to this actor _via_ Kafka? We need a `KafkaActorRef`. This is an
abstraction over producing messages to Kafka. Declaring one is pretty simple:

```scala
val exampleConsumerRef = new KafkaActorRef(
  bootstrapServers = "localhost:9092",
  kafkaTopic = "kafka-actors-example-consumer"
)
```

Then, we just send the message to the actor:

```scala
exampleConsumerRef ! Ping()
```

This instance of ping will be produced to the Kafka topic and consumed on by the `KafkaActor` on
the other end.

If the actor that you would like to send a message to is actually available in the current process,
but you just want to take advantage of the Kafka-backed functionality, you can use the `ref` that
comes packaged with the `KafkaActor`.

```scala
ExampleConsumer.ref ! Ping()
```

This will route the `Ping` message through a pre-build `KafkaActorRef` that goes to Kafka and the
message will then be consumed by the actor during the next Kafka consumption poll.

## Limitations and Roadmap

Some things we'd like to get done on this next:

* Currently, this requires that your message be serializable with the `DefaultFormats` of lift-json.
  Support for custom serializers would be a huge win.
* Messages are also required to have a type hint to be deserialized. This is well suited for
  different parts of the same Scala application that need to talk to each other, but not so well
  suited for messages that may originate from a totally different stack. We would eventually like
  to provide hooks where more specialized serialization behavior could be applied for cases where
  there's a multi-stack environment in play.
* Improve the test coverage.
* Support at most once processing in addition to at least once processing.

## Contributing

This project is governed by [Lift's Contributing Guidelines][lcg]. Support for a release of this
Lift Module will roughly follow [Lift's Support Schedule][lss] for the release of Lift that the
version was built against, with some minor alterations. In summary:

* We'll continue to deliver new functionality for the current version of Lift only.
* We originally built this for Lift 3.1, and will not back-port it to older editions.
* Minor fixes will be delivered for old versions of this module _if_ the edition of Lift it was
  built against is still receiving minor fixes.
* Security fixes will be delivered for old versions of this module _if_ the edition of Lift it
  was built against is still receiving security fixes.

[lcg]: https://github.com/lift/framework/blob/master/CONTRIBUTING.md
[lss]: https://github.com/lift/framework/blob/master/SUPPORT.md

## License

This project is licensed under the Apache 2 license.
