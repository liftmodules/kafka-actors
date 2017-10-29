package net.liftmodules.kafkaactors

import net.liftweb.common._

case class Ping()

object ExampleConsumer extends KafkaActor with Loggable {
  override val bootstrapServers = "localhost:9092"
  override val kafkaTopic = "kafka-actors-example-consumer"
  override val groupId = "kafka-actors-example-consumer"
  override val pollTime = 1000L

  override def userMessageHandler = {
    case Ping() =>
      logger.info("Got ping!")
  }
}

object ExampleConsumerApp extends App with Loggable {
  logger.info("Starting consumer...")
  ExampleConsumer ! StartConsumer
  Thread.sleep(500)
  logger.info("Main thread is done sleeping")
}
