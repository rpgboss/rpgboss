package rpgboss.model

import org.json4s._
import org.json4s.native.Serialization
import rpgboss.UnitSpec

case class Car(model: String, passengers: Array[String])

class JsonSpec extends UnitSpec {
  implicit val formats = Serialization.formats(NoTypeHints)

  "Json serialization" should "work for Arrays" in {
    val model = Car("Ford", Array("Alice", "Bob"))
    val ser = Serialization.write(model)
    val deserializedModel = Serialization.read[Car](ser)
    deserializedModel should deepEqual (model)
  }
}