package net.liftmodules.kafkaactors

import java.util.Properties
import org.apache.kafka.clients.producer._
import net.liftweb.actor._

/**
 * A ref to a KafkaActor that will send its message through Kafka.
 */
abstract class KafkaActorRef extends SpecializedLiftActor[KafkaActorMessage] {
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

  override def !(message: KafkaActorMessage): Unit = {
    val envelope = KafkaMessageEnvelope(message)
    val record = new ProducerRecord[Array[Byte], KafkaMessageEnvelope](kafkaTopic, envelope)

    producer.send(record)
  }

  override def messageHandler = {
    case _ =>
  }
}
