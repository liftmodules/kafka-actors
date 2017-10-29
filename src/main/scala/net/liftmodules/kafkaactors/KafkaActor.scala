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
import org.apache.kafka.clients.consumer._
import net.liftweb.actor._
import scala.collection.JavaConverters._

/**
 * A kind of LiftActor capable of consuming messages from a Kafka topic.
 *
 * This actor imposes a few restrictions that normal LiftActors do not. Specifically:
 *
 * - You must define your message handling in userMessageHandler instead of messageHandler.
 * - You cannot override the processing of any InternalKafkaActorMessage.
 *
 * Other than the above, this Actor behaves very similarly to a normal LiftActor.
 * You can send messages directly to it, thus bypassing Kafka, by using its `!` or `send` methods.
 *
 * You will need to override a few values to have it work correctly:
 *
 * - bootstrapServers: This needs to be the broker list for your Kafka cluster
 * - groupId: This is the groupId the actor should consume under. See Kafka docs for more details.
 * - kafkaTopic: The topic the actor should subscribe to
 * - pollTime: The amount of time, in milliseconds, the consumer should wait for records before
 *   looping to handle housecleaning tasks.
 *
 * Once you've implemented and instantiated an instance of your class, you'll need to send the
 * message `SartConsumer` to actually connect to and start consuming messages from Kafka.
 */
abstract class KafkaActor extends LiftActor {
  def bootstrapServers: String
  def groupId: String
  def kafkaTopic: String
  def pollTime: Long

  /**
   * Override this method in the implementing class to customize the consumer settings
   * to your liking.
   */
  def consumerPropsCustomizer(props: Properties): Properties = props

  def consumerFn(): KafkaConsumer[Array[Byte], KafkaMessageEnvelope] = {
    val props = new Properties()
    props.put("bootstrap.servers", bootstrapServers)
    props.put("group.id", groupId)
    props.put("enable.auto.commit", "false")
    props.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
    props.put("value.deserializer", "net.liftmodules.kafkaactors.KafkaMessageEnvelopeDeserializer")

    val customizedProps = consumerPropsCustomizer(props)
    val consumer = new KafkaConsumer[Array[Byte], KafkaMessageEnvelope](customizedProps)

    consumer.subscribe(List(kafkaTopic).asJava)
    consumer
  }

  protected lazy val consumingThread: KafkaActorConsumingThread = new KafkaActorConsumingThread(
    groupId + "-consuming-thread",
    consumerFn,
    this,
    pollTime
  )

  final override def messageHandler = {
    case internalMessage: InternalKafkaActorMessage =>
      internalMessage match {
        case StartConsumer =>
          consumingThread.start()

        case StopConsumer =>
          consumingThread.shutdown()

        case CommitOffsets(offsetInfo) =>
          consumingThread.addPendingOffsets(offsetInfo)
      }

    case userMessage =>
      userMessageHandler(userMessage)
  }

  def userMessageHandler: PartialFunction[Any, Any]
}
