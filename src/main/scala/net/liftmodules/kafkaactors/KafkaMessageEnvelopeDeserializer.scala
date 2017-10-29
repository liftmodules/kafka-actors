package net.liftmodules.kafkaactors

import java.util.{Map => JMap}
import org.apache.kafka.common.serialization._
import net.liftweb.json._

class KafkaMessageEnvelopeDeserializer extends Deserializer[KafkaMessageEnvelope] {
  implicit val formats = DefaultFormats

  override def close(): Unit = ()

  override def configure(configs: JMap[String, _], isKey: Boolean): Unit = ()

  override def deserialize(topic: String, data: Array[Byte]): KafkaMessageEnvelope = {
    val jsonString = new String(data, "UTF-8")
    val parsedJson = parse(jsonString)
    KafkaMessageEnvelope(parsedJson)
  }
}
