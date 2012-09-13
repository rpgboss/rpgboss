package rpgboss.player.entity
import com.badlogic.gdx.graphics.g2d.SpriteBatch

trait Entity {
  def update()
  def render(batch: SpriteBatch)
  
  var deleted = false
  def delete() = deleted = true
}

trait NamedEntity extends Entity {
  def name: String
}
