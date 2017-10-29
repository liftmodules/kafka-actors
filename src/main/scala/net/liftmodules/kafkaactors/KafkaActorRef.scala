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

import java.util.Properties
import org.apache.kafka.clients.producer._
import net.liftweb.actor._

/**
 * A ref to a KafkaActor that will send its message through Kafka.
 *
 * This class conforms to the LiftActor shape so that it can be plugged into anything that would
 * normally take a LiftActor. However, as we've provided a custom implementation of the send method
 * this actor won't be able to define a working messageHandler.
 */
abstract class KafkaActorRef extends LiftActor {
  def bootstrapServers: String
  def kafkaTopic: String
  def acks: String = "all"
  def retries: String = "100"

  lazy val producer: Producer[Array[Byte], KafkaMessageEnvelope] = {
    val props = new Properties()
    props.put("bootstrap.servers", bootstrapServers)
    props.put("acks", acks)
    props.put("retries", retries)
    props.put("batch.size", "16384")
    props.put("linger.ms", "1")
    props.put("buffer.memory", "33554432")
    props.put("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")
    props.put("value.serializer", "net.liftmodules.kafkaactors.KafkaMessageEnvelopeSerializer")

    new KafkaProducer(props)
  }

  override def !(message: Any): Unit = {
    val envelope = KafkaMessageEnvelope(message)
    val record = new ProducerRecord[Array[Byte], KafkaMessageEnvelope](kafkaTopic, envelope)

    producer.send(record)
  }

  final override def messageHandler = {
    case _ =>
  }
}
