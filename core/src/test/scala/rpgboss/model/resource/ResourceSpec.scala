package rpgboss.model.resource

import rpgboss._

class ResourceSpec extends UnitSpec {
  "Resource" should "resolve metadata paths correctly" in {
    Autotile.metadataPathRelative("sys/testname.png") should equal (
        "sys/testname.png.metadata.json")
  }
}