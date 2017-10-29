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
 *  - You must define your message handling in `userMessageHandler` instead of `messageHandler`.
 *  - You cannot override the processing of any `InternalKafkaActorMessage`.
 *
 * Other than the above, this Actor behaves very similarly to a normal `LiftActor`.
 * You can send messages directly to it, thus bypassing Kafka, by using its `!` or `send` methods.
 *
 * == Configuration ==
 *
 * For this actor to work correctly with Kafka, you'll ahve to provide it with some basic
 * configuration. The required overrides are:
 *
 *  - `bootstrapServers`: This needs to be the broker list for your Kafka cluster
 *  - `groupId`: This is the groupId the actor should consume under. See Kafka docs for more details.
 *  - `kafkaTopic`: The topic the actor should subscribe to
 *
 * The Kafka consumer works by polling for a certain number of milliseconds, and then returning if
 * no messages are retrieved. We abstract away that event loop behavior, but sometimes applications
 * need to tweak how long the consumer will sleep in order to optimize performance. To change that
 * you can also override the following:
 *
 *  - `pollTime`: The amount of time, in milliseconds, the consumer should wait for recrods. Defaults to 500ms.
 *
 * If you need to tune more specific settings, you can provide a `consumerPropsCustomizer` that
 * will get to alter the `Properties` object before we pass it into the `KafkaConsumer` constructor.
 * This is what you'll need to implement if you want to provide custom settings for things like
 * authentication, encryption, etc. By default, we provide the bootstrap servers, the group ID,
 * we disable auto committing, and provide a key and value serializer implementation.
 *
 * Please be careful when overriding settings that were set by the `KafkaActor` itself.
 *
 * == Starting consumption ==
 *
 * Once the actor is created, it'll behave like a normal `LiftActor` until its told to connect
 * up to Kafka and start consuming messages. To do that your code will need to transmit the
 * `[[StartConsumer]]` message to it like so:
 *
 * {{{
 * actor ! StartConsumer
 * }}}
 *
 * You can also stop consuming anytime you like by transmitting `[[StopConsumer]]` or you can
 * force committing offsets by transmitting `[[CommitOffsets]]` to the actor if you need to do
 * so for some reason, though as mentioned below those cases should be rare.
 *
 * == Processing messages ==
 *
 * When messages come from the topic, they will be parsed and extracted to case class objects
 * using lift-json. The messages will then be put in the actor's normal mailbox using `!` and be
 * subjet to normal actor processing rules. Every time the actor consumes messages it'll also
 * add a `[[CommitOffsets]]` message onto the end of the message batch.
 *
 * Because of the way the actor mailbox works, `CommitOffsets` won't be processed until all of
 * the messages in that batch have been processed. Thus, if you have a class of errors that may
 * cause you to want to avoid checkpointing offsets to Kafka, you sould throw an exception of
 * some sort in your `userMessageHandler` so things blow up.
 *
 */
abstract class KafkaActor extends LiftActor {
  def bootstrapServers: String
  def groupId: String
  def kafkaTopic: String
  def pollTime: Long = 500L

  /**
   * Override this method in the implementing class to customize the consumer settings
   * to your liking.
   */
  def consumerPropsCustomizer(props: Properties): Properties = props

  private[this] def consumerFn(): KafkaConsumer[Array[Byte], KafkaMessageEnvelope] = {
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
