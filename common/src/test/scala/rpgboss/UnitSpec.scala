package rpgboss

import org.scalatest._
import org.scalatest.matchers._
import org.json4s._
import org.json4s.native.Serialization

class UnitSpec extends FlatSpec with Matchers {
  // Case class equality tester that handles array equality correctly
  class DeepEqualMatcher[A](expected: A) 
    extends Matcher[A] {
    
    def deepEquals(a: Product, b: Product): Boolean = {
      assume(a.productArity == b.productArity)
      val equalityList = for (i <- 0 until a.productArity) yield {
        (a.productElement(i), b.productElement(i)) match {
          case (x: Product, y: Product) => deepEquals(x, y)
          case (x: Array[_], y: Array[_]) => x.sameElements(y)
          case (x, y) => x == y
        }
      }
      
      !equalityList.contains(false)
    }
    
    def apply(left: A) = {
      val equals = 
        if (left.isInstanceOf[Product] && expected.isInstanceOf[Product]) {
          deepEquals(left.asInstanceOf[Product], expected.asInstanceOf[Product])
        } else {
          left == expected
        }
      
      MatchResult(
        equals,
        s"""$left did not deepEqual $expected""",
        s"""$left deepEqualed $expected""")
    }
  }
  
  def deepEqual[A](right: A) = new DeepEqualMatcher(right)
}