package net.liftmodules.kafkaactors

import java.io._
import net.liftweb.json._
import net.liftweb.json.Extraction._

private[kafkaactors] case class KafkaMessageEnvelope(
  decomposedData: JValue
)

private[kafkaactors] object KafkaMessageEnvelope {
  implicit val formats = DefaultFormats + FullTypeHints(classOf[Object] :: Nil)

  def apply[T](data: T)(implicit manifest: Manifest[T]): KafkaMessageEnvelope = {
    apply(decompose(data))
  }

  def extract[T](envelope: KafkaMessageEnvelope)(implicit manifest: Manifest[T]): T = {
    envelope.decomposedData.extract[T]
  }
}
