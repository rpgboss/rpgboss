package rpgboss

import org.scalatest._
import org.scalatest.matchers._

class UnitSpec extends FlatSpec with Matchers {
  /**
   * This exists so we can correctly compare case classes with array members.
   * TODO: Investigate if we can do this without using json4s, which might not
   * be always accurate.
   */
  class DeepEqualMatcher[A <: AnyRef](expected: A) 
    extends Matcher[A] {
    
    def deepEquals[T](a: T, b: T): Boolean = {
      if (a.isInstanceOf[Product] && b.isInstanceOf[Product]) {
        val aProduct = a.asInstanceOf[Product]
        val bProduct = b.asInstanceOf[Product]
        if (aProduct.productArity != bProduct.productArity)
          return false
        
        val pairIt = (aProduct.productIterator zip bProduct.productIterator)
        pairIt forall {
          case (aElement, bElement) => {
            deepEquals(aElement, bElement)
          }
        }
      } else if (a.isInstanceOf[Array[_]] && b.isInstanceOf[Array[_]]) {
        val aArray = a.asInstanceOf[Array[_]]
        val bArray = b.asInstanceOf[Array[_]]
        if (aArray.length != bArray.length)
          return false
        
        val pairIt = aArray zip bArray
        pairIt forall {
          case (aElement, bElement) => {
            deepEquals(aElement, bElement)
          }
        }
      } else if (a.isInstanceOf[Map[_, _]] && b.isInstanceOf[Map[_, _]]) {
        val aMap = a.asInstanceOf[Map[_, _]]
        val bMap = b.asInstanceOf[Map[_, _]]
        if (aMap.size != bMap.size)
          return false
        
        // TODO: Slow, but probably sufficient.
        aMap.iterator.forall {
          case (aK, aV) => bMap.find({
            case (bK, bV) => aK == bK && deepEquals(aV, bV)
          }).isDefined
        }
      } else {
        val equal = a == b
        if (!equal)
          println("  Not equal: \n    %s\n    %s".format(a, b))
        equal
      }
    }
    
    def apply(left: A) = {
      MatchResult(
        deepEquals(left, expected),
        s"""$left did not deepEqual $expected""",
        s"""$left deepEqualed $expected""")
    }
  }
  
  def deepEqual[A <: AnyRef](right: A) = new DeepEqualMatcher(right)
}