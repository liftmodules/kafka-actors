package net.liftmodules.kafkaactors

import org.apache.kafka.common._
import org.apache.kafka.clients.consumer._
import java.util.{Map => JMap}

sealed trait InternalKafkaActorMessage

/**
 * Signals for the consumer thread to commit offsets.
 */
case class CommitOffsets(
  offsets: JMap[TopicPartition, OffsetAndMetadata]
) extends InternalKafkaActorMessage

case object StartConsumer extends InternalKafkaActorMessage

case object StopConsumer extends InternalKafkaActorMessage
