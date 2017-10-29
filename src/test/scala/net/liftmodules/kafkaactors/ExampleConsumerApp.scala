/**
 * Copyright 2017 WorldWide Conferencing, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
