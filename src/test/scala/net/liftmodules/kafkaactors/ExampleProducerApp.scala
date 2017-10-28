package net.liftmodules.kafkaactors

import net.liftweb.common._

object ExampleConsumerRef extends KafkaActorRef with Loggable {
  override val bootstrapServers = "localhost:9092"
  override val kafkaTopic = "kafka-actors-example-consumer"
}

object ExampleProducerApp extends App with Loggable {
  logger.info("Starting producer...")
  ExampleConsumerRef ! Ping()
  Thread.sleep(1000)
  logger.info("Send complete")
}
