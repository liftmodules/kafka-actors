package net.liftmodules.kafkaactors

import java.io._
import net.liftweb.json._
import net.liftweb.json.Extraction._

case class KafkaMessageEnvelope(
  manifestBytes: Array[Byte],
  decomposedData: JValue
)

object KafkaMessageEnvelope {
  implicit val formats = DefaultFormats + FullTypeHints(classOf[KafkaActorMessage] :: Nil)

  def apply[T](data: T)(implicit manifest: Manifest[T]): KafkaMessageEnvelope = {
    val baos = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(baos)

    try {
      oos.writeObject(manifest)
      val manifestBytes = baos.toByteArray()

      val decomposedData = decompose(data)
      apply(manifestBytes, decomposedData)
    } finally {
      oos.close()
      baos.close()
    }
  }

  def extract[T](envelope: KafkaMessageEnvelope): T = {
    val bais = new ByteArrayInputStream(envelope.manifestBytes)
    val ois = new ObjectInputStream(bais)

    try {
      implicit val manifest: Manifest[T] = ois.readObject().asInstanceOf[Manifest[T]]
      envelope.decomposedData.extract[T]
    } finally {
      ois.close()
      bais.close()
    }
  }
}
