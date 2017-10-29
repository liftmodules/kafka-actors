package net.liftmodules.kafkaactors

import java.io._
import net.liftweb.json._
import net.liftweb.json.Extraction._

case class KafkaMessageEnvelope(
  decomposedData: JValue
)

object KafkaMessageEnvelope {
  implicit val formats = DefaultFormats + FullTypeHints(classOf[Object] :: Nil)

  def apply[T](data: T)(implicit manifest: Manifest[T]): KafkaMessageEnvelope = {
    apply(decompose(data))
  }

  def extract[T](envelope: KafkaMessageEnvelope)(implicit manifest: Manifest[T]): T = {
    envelope.decomposedData.extract[T]
  }
}
