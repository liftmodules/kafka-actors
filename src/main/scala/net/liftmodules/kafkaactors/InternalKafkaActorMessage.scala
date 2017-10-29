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

import org.apache.kafka.common._
import org.apache.kafka.clients.consumer._
import java.util.{Map => JMap}

/**
 * This is the parent trait for messages that KafkaActors handle internally. It's not possible
 * for user code to intercept any messages that subclass this trait.
 */
sealed trait InternalKafkaActorMessage

/**
 * Tells the actor to signal the consumer thread to commit the enclosed offsets.
 *
 * This message is essentially a checkpoint for consumption. Once the offsets are committed
 * the messages before that point won't be reconsumed if the actor crashes and restarts.
 */
case class CommitOffsets(
  offsets: JMap[TopicPartition, OffsetAndMetadata]
) extends InternalKafkaActorMessage

/**
 * Instruction for the KafkaActor to start consuming messages from Kafka.
 *
 * User code must send this message to newly created Kafka actors to cause them to start
 * consuming from Kafka. Until this message is sent, a KafkaActor is no more than a regular
 * LiftActor. If you wish to have Kafka-consuming behavior toggleable, you should be able to
 * add code paths that do or don't send this message to the relevant actor.
 */
case object StartConsumer extends InternalKafkaActorMessage

/**
 * Instruction for the KafkaActor to stop consuming messages from Kafka.
 *
 * We recommend sending this message once your application knows its going to shut down so that
 * consumption can finish up cleanly.
 */
case object StopConsumer extends InternalKafkaActorMessage
