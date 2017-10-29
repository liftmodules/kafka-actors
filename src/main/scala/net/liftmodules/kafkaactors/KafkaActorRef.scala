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
import net.liftweb.common._
import net.liftweb.actor._

/**
 * A ref to a KafkaActor that will send its message through Kafka.
 *
 * This class conforms to the LiftActor shape so that it can be plugged into anything that would
 * normally take a LiftActor. However, as we've provided a custom implementation of the send method
 * this actor won't be able to define a working messageHandler.
 *
 * By default, this producer is configured to ensure all brokers acknowledge a message and to
 * ensure that requests are properly ordered. It's also configured with 10 retries by default.
 * If you'd like to customize these settings, you can override {{producerPropsCustomizer}} to
 * change the {{Properties}} instance that we use to configure the producer.
 *
 * @param bootstrapServers The kafka broker list to connect to in the form of: "host1:port,host2:port,..."
 * @param kafkaTopic The kafka topic to produce to.
 */
abstract class KafkaActorRef(bootstrapServers: String, kafkaTopic: String) extends LiftActor with Loggable with Tryo {
  /**
   * Override this method in the implementing class to customize the producer settings
   * to your liking.
   */
  def producerPropsCustomizer(props: Properties): Properties = props

  /**
   * Override this method to provide custom success and error handling when the producer has
   * finished its round trip to the broker. The default implementation will simply log an
   * exception if one occurs and prevent it from being re-thrown.
   *
   * If ensuring delivery is super-important to your application, you may wish to override
   * this to cause your application to crash if producing isn't working.
   */
  def onCompletionCallback(metadata: RecordMetadata, exception: Exception): Unit = {
    if (Option(exception).isDefined) {
      logger.error("An exception occurred while trying to produce messages to Kafka", exception)
    }
  }

  /**
   * Override this method to provide custom error handling when an exception is thrown from
   * the producer's send method. The default implementation will simply log the exception and
   * prevent it from being re-thrown.
   *
   * Please refer to the Kafka documentation for the current kinds of exception the Kafka
   * Producer's send method may return.
   */
  def onProducerException(exception: Exception): Unit = {
    logger.error("An exception occurred while trying to produce messages to Kafka", exception)
  }

  lazy val producer: Producer[Array[Byte], KafkaMessageEnvelope] = {
    logger.info("Creating producer")

    val props = new Properties()
    props.put("bootstrap.servers", bootstrapServers)
    props.put("acks", "all")
    props.put("retries", "10")
    props.put("max.in.flight.requests.per.connection", "1")
    props.put("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")
    props.put("value.serializer", "net.liftmodules.kafkaactors.KafkaMessageEnvelopeSerializer")

    val customizedProps = producerPropsCustomizer(props)

    new KafkaProducer(customizedProps)
  }

  private[this] lazy val producerCallback: Callback = new Callback {
    override def onCompletion(metadata: RecordMetadata, exception: Exception) =
      onCompletionCallback(metadata, exception)
  }

  override def !(message: Any): Unit = {
    val envelope = KafkaMessageEnvelope(message)
    val record = new ProducerRecord[Array[Byte], KafkaMessageEnvelope](kafkaTopic, envelope)

    tryo(producer.send(record, producerCallback)) match {
      case Failure(_, Full(exception: Exception), _) =>
        onProducerException(exception)

      case _ =>
    }
  }

  final override def messageHandler = {
    case _ =>
  }
}
