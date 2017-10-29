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

import java.util.{Map => JMap}
import org.apache.kafka.common.serialization._
import net.liftweb.json._

private[kafkaactors] class KafkaMessageEnvelopeDeserializer extends Deserializer[KafkaMessageEnvelope] {
  implicit val formats = DefaultFormats

  override def close(): Unit = ()

  override def configure(configs: JMap[String, _], isKey: Boolean): Unit = ()

  override def deserialize(topic: String, data: Array[Byte]): KafkaMessageEnvelope = {
    val jsonString = new String(data, "UTF-8")
    val parsedJson = parse(jsonString)
    KafkaMessageEnvelope(parsedJson)
  }
}
