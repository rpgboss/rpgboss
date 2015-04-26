Breaking Changes
================

These changes may require you to manually edit the JSON files in existing projects to prevent losing data. Or, you may copy the version of the JSON file generated in a new project to your existing project.

v0.1.7
------
 - In "enemies.json", enemy skills field has been renamed from "skills" to "skillIds".

v0.2.9
------
 - In "items.json", the item type field has been renamed from "itemType" to "itemTypeId".

v0.3.3
------
 - Some battle backgrounds have been moved. This may break event commands that start battles.

v0.5.4
------
 - (Very) old Events with an EndOfScript marker will no longer deserialize correctly.

v0.8.5
------
 - In "items.json", the on attack skill field has been renamed from "onUseSkillId" to "equippedAttackSkillId".
