package net.liftmodules.kafkaactors

import java.util.{Map => JMap}
import org.apache.kafka.common.serialization._
import net.liftweb.json.{Serializer => _, _}
import net.liftweb.json.Extraction._

private[kafkaactors] class KafkaMessageEnvelopeSerializer extends Serializer[KafkaMessageEnvelope] {
  implicit val formats = DefaultFormats

  override def close(): Unit = ()

  override def configure(configs: JMap[String, _], isKey: Boolean): Unit = ()

  override def serialize(topic: String, envelope: KafkaMessageEnvelope): Array[Byte] = {
    compactRender(envelope.decomposedData).getBytes("UTF-8")
  }
}
