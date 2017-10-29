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

  it should "support generic case glasses" in {
    assertIdentity(ExperimentalCaseClass3[String]("apple"))
  }

  it should "support parent traits" in {
    assertIdentity[ExperimentalParentTrait](ExperimentalCaseClass4("abcd"))
  }

  private[this] def assertIdentity[T](thing: T)(implicit mf: Manifest[T]) = {
    val envelope = KafkaMessageEnvelope(thing)
    val extracted = KafkaMessageEnvelope.extract[T](envelope)

    extracted should equal(thing)
  }
}

case class ExperimentalCaseClass1(name: String, number: Int)
case class ExperimentalCaseClass2(name: String, child: ExperimentalCaseClass1)
case class ExperimentalCaseClass3[T](name: T)

trait ExperimentalParentTrait
case class ExperimentalCaseClass4(name: String) extends ExperimentalParentTrait
