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
