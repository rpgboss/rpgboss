package rpgboss.model

import rpgboss._
import rpgboss.lib._
import com.google.common.io.Files

class EffectSpec extends UnitSpec {
  "Effect" should "have a working meta system" in {
    Effect(100, 5).meta should equal(RecoverHpAdd)
  }
}