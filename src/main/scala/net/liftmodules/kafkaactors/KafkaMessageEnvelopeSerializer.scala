package net.liftmodules.kafkaactors

import java.util.{Map => JMap}
import org.apache.kafka.common.serialization._
import net.liftweb.json.{Serializer => _, _}
import net.liftweb.json.Extraction._

class KafkaMessageEnvelopeSerializer extends Serializer[KafkaMessageEnvelope] {
  implicit val formats = DefaultFormats

  override def close(): Unit = ()

  override def configure(configs: JMap[String, _], isKey: Boolean): Unit = ()

  override def serialize(topic: String, data: KafkaMessageEnvelope): Array[Byte] = {
    compactRender(decompose(data)).getBytes("UTF-8")
  }
}
