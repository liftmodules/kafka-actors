package net.liftmodules.kafkaactors

import org.apache.kafka.common._
import org.apache.kafka.clients.consumer._
import java.util.{Map => JMap}

/**
 * The parent trait for all KafkaActorMessages. Any user messages that originate from
 * Kafka must subclass this type.
 */
trait KafkaActorMessage

sealed trait InternalKafkaActorMessage extends KafkaActorMessage

/**
 * Signals for the consumer thread to commit offsets.
 */
case class CommitOffsets(
  offsets: JMap[TopicPartition, OffsetAndMetadata]
) extends InternalKafkaActorMessage

case object StartConsumer extends InternalKafkaActorMessage

case object StopConsumer extends InternalKafkaActorMessage
