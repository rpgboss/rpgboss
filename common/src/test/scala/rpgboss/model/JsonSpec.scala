package rpgboss.model

import org.json4s._
import org.json4s.native.Serialization
import rpgboss.UnitSpec

class JsonSpec extends UnitSpec {
  implicit val formats = Serialization.formats(NoTypeHints)
  
  "Json serialization" should "work for Arrays" in {
    case class Car(model: String, passengers: Array[String])
    val model = Car("Ford", Array("Alice", "Bob"))
    val ser = Serialization.write(model)
    Serialization.read[Car](ser) should equal (model)
  }
}