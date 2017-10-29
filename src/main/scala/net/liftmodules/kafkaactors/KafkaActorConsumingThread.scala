package net.liftmodules.kafkaactors

import java.lang.Thread
import java.util.concurrent._
import java.util.concurrent.atomic._
import java.util.{Map => JMap, HashMap => JHashMap}
import org.apache.kafka.common._
import org.apache.kafka.common.errors._
import org.apache.kafka.clients.consumer._
import net.liftweb.actor._
import net.liftweb.common.Loggable
import scala.collection.JavaConverters._

/**
 * Kafka consumers ensure that only one thread is consuming from the client at
 * once. The KafkaActorConsumingThread abstraction makes that possible. This
 * abstraction will receive messages from the Kafka broker, deserialize them,
 * and then pass them into the actor as if it was a normal message.
 *
 * This class also wraps a bit of internal state tracking around committing
 * offsets to ensure that offsets are committed after the actor using this
 * thread has successfully processed the batch of messages. To do this we
 * tack an extra message, CommitOffsets, onto the end that will cause the actor
 * to signal this thread that those offsets are ready to commit.
 */
class KafkaActorConsumingThread(
  name: String,
  consumerFn: ()=>KafkaConsumer[Array[Byte], KafkaMessageEnvelope],
  parentActor: KafkaActor,
  pollTime: Long
) extends Thread(name) with Loggable {
  private val closed = new AtomicBoolean(false)

  private object PendingOffsetsLock
  private[this] var pendingOffsetCommit: JMap[TopicPartition, OffsetAndMetadata] = new JHashMap()

  private[this] var consumer: Option[KafkaConsumer[Array[Byte], KafkaMessageEnvelope]] = None

  override def run() = {
    closed.set(false)
    consumer = Some(consumerFn())

    try {
      while (! closed.get()) {
        if (consumer.isEmpty) {
          throw new IllegalStateException("Consumer has somehow become None. Aborting consumption.")
        }

        commitAnyPendingOffsets()

        val records = consumer match {
          case Some(consumer) =>
            consumer.poll(pollTime).asScala

          case None =>
            throw new IllegalStateException("Consumer has somehow become None. Aborting consumption.")
        }

        val currentIterationOffsets: JMap[TopicPartition, OffsetAndMetadata] = new JHashMap()

        for (record <- records) {
          val envelope = record.value()
          val actorMessage = KafkaMessageEnvelope.extract[Any](envelope)

          parentActor ! actorMessage

          val topicPartitionForRecord = new TopicPartition(record.topic(), record.partition())
          val nextOffset = new OffsetAndMetadata(record.offset() + 1)
          currentIterationOffsets.put(topicPartitionForRecord, nextOffset)
        }

        if (records.nonEmpty)
          parentActor ! CommitOffsets(currentIterationOffsets)
      }
    } catch {
      case e: WakeupException =>
        if (! closed.get())
          throw e
    }
  }

  def shutdown() {
    closed.set(true)
    consumer.foreach(_.wakeup())
  }

  def addPendingOffsets(newOffsets: JMap[TopicPartition, OffsetAndMetadata]): Unit = {
    PendingOffsetsLock.synchronized {
      newOffsets.asScala.foreach {
        case (topicPartition, offset) =>
          pendingOffsetCommit.put(topicPartition, offset)
      }
    }
  }

  /**
   * This is the callback for offset commiting. It will execute whenever a commit offsets request
   * has completed an log the status of the offset committing - at ERROR level when exceptions
   * occurs and at DEBUG level otherwise.
   */
  private[this] val offsetCommitCallback = new OffsetCommitCallback {
    override def onComplete(offsets: JMap[TopicPartition, OffsetAndMetadata], exception: Exception) = {
      if (exception != null) {
        logger.error(s"Exception while committing offsets", exception)
      } else {
        logger.debug(s"Offsets were committed successfully")
      }

      PendingOffsetsLock.synchronized {
        pendingOffsetCommit.clear()
      }
    }
  }

  private[this] def commitAnyPendingOffsets() = {
    PendingOffsetsLock.synchronized {
      if (! pendingOffsetCommit.isEmpty) {
        for (consumer <- consumer) {
          consumer.commitAsync(pendingOffsetCommit, offsetCommitCallback)
        }
      }
    }
  }
}
