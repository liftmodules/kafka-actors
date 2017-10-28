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
 * - All message must subtype KafkaActorMessage and be serializable with lift-json.
 * - You must define your message handling in userMessageHandler.
 * - You cannot override the processing of any InternalKafkaActorMessage such as:
 *   - StartConsumer
 *   - StopConsumer
 *   - CommitOffsets
 * - You may, however, manually send those messages at any time.
 *
 * Other than the above, this Actor behaves very similarly to a normal SpecializedLiftActor.
 * You can send messages directly to it, thus bypassing Kafka, by using its `!` or `send` methods.
 *
 * You will need to override a few values to have it work correctly:
 *
 * - bootstrapServers: This needs to be the broker list for your Kafka cluster
 * - groupId: This is the groupId the actor should consume under. See Kafka docs for more details.
 * - kafkaTopic: The topic the actor should subscribe to
 * - pollTime: The amount of time, in milliseconds, the consumer should wait for records before
 *   looping to handle housecleaning tasks.
 */
abstract class KafkaActor extends SpecializedLiftActor[KafkaActorMessage] {
  def bootstrapServers: String
  def groupId: String
  def kafkaTopic: String
  def pollTime: Long

  def consumerFn(): KafkaConsumer[Array[Byte], KafkaMessageEnvelope] = {
    val props = new Properties()
    props.put("bootstrap.servers", bootstrapServers)
    props.put("group.id", groupId)
    props.put("enable.auto.commit", "false")
    props.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
    props.put("value.deserializer", "net.liftmodules.kafkaactors.KafkaMessageEnvelopeDeserializer")
    val consumer = new KafkaConsumer[Array[Byte], KafkaMessageEnvelope](props)

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

  def userMessageHandler: PartialFunction[KafkaActorMessage, Any]
}
