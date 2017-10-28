package net.liftmodules.kafkaactors

import org.scalatest._

class KafkaMessageEnvelopeSpec extends FlatSpec with Matchers {
  "KafkaMessageEnvelope" should "support scala primitives" in {
    assertIdentity(100)
    assertIdentity(100L)
    assertIdentity(true)
    assertIdentity(10.10)
  }

  it should "support strings" in {
    assertIdentity("bacon")
  }

  it should "support simple case classes" in {
    assertIdentity(ExperimentalCaseClass1("bacon", 10))
  }

  it should "support nested case classes" in {
    assertIdentity(ExperimentalCaseClass2("applesuce", ExperimentalCaseClass1("bacon", 10)))
  }

  private[this] def assertIdentity[T](thing: T)(implicit mf: Manifest[T]) = {
    val envelope = KafkaMessageEnvelope(thing)
    val extracted = KafkaMessageEnvelope.extract[T](envelope)

    extracted should equal(thing)
  }
}

case class ExperimentalCaseClass1(name: String, number: Int)
case class ExperimentalCaseClass2(name: String, child: ExperimentalCaseClass1)
