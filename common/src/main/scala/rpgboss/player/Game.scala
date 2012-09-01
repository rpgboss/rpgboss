package rpgboss.player

import com.badlogic.gdx.ApplicationListener
import java.io.File
import rpgboss.model._
import com.badlogic.gdx.graphics._
import com.badlogic.gdx.graphics.g2d._
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Matrix4

class Game(gamepath: File) extends ApplicationListener {
  val project = Project.readFromDisk(gamepath).get 
  var map: RpgMap = null
  var mapData: RpgMapData = null
  var camera: OrthographicCamera = null
  var tilesets: Array[Tileset] = null
  var tilesetTexs: Array[Texture] = null
  var batch: SpriteBatch = null
  
  var atlas: TextureAtlas = null
  
  // in units of tiles
  var screenW = 20.0
  var screenH = 15.0
  
  // camera position and boundaries
  var cameraLoc: MapLoc = null
  var cameraL: Double = 0
  var cameraR: Double = 0
  var cameraT: Double = 0
  var cameraB: Double = 0
  
  def setCameraLoc(loc: MapLoc) = {
    cameraLoc = loc
    cameraL = loc.x - screenW/2
    cameraR = loc.x + screenW/2
    cameraT = loc.y - screenH/2
    cameraB = loc.y + screenH/2
    camera.position.x = loc.x
    camera.position.y = loc.y
    camera.update()
  }
  
  override def create() {
    map = RpgMap.readFromDisk(project, project.data.startingLoc.map)
    mapData = map.readMapData().get
    
    camera = new OrthographicCamera()
    camera.setToOrtho(true, screenW.toFloat, screenH.toFloat) // y points down
    
    setCameraLoc(project.data.startingLoc)
    
    val packer = new PixmapPacker(1024, 1024, Pixmap.Format.RGBA8888, 0, false)
    
    // Pack all the autotiles
    project.data.autotiles.map { name =>
      val autotile = Autotile.readFromDisk(project, name)
      val autotilePix = new Pixmap(
          Gdx.files.absolute(autotile.dataFile.getAbsolutePath()))
      
      packer.pack("autotiles/%s".format(name), autotilePix)

      // No need to dispose of pixmaps, I believe, as they get disposed of
      // when the TextureAtlas gets disposed
      
    }
    
    tilesets = map.metadata.tilesets.map(
        name => Tileset.readFromDisk(project, name)).toArray[Tileset]
    
    tilesetTexs = tilesets.map(
        x => new Texture(Gdx.files.absolute(x.dataFile.getAbsolutePath())))
    
    
    /*
     * SpriteBatch manages its own matrices. By default, it sets its modelview
     * matrix to the identity, and the projection matrix to an orthographic
     * projection with its lower left corner of the screen at (0, 0) and its
     * upper right corner at (Gdx.graphics.getWidth(), Gdx.graphics.getHeight())
     * 
     * This makes the eye-coordinates the same as the screen-coordinates.
     * 
     * If you'd like to specify your objects in some other space, simply
     * change the projection and modelview (transform) matrices.
     */
    batch = new SpriteBatch() 
  }
  override def render() {
    // Clear the context
    Gdx.gl.glClearColor(0, 0, 0, 1)
    Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT)
    
    // Set the projection matrix to the combined camera matrices
    // This seems to be the only thing that works...
    batch.setProjectionMatrix(camera.combined)
    batch.begin()
    
    // Leftmost, rightmost, topmost, bottom-most tiles to render
    val tileL = math.max(0, cameraL.toInt)
    val tileR = math.min(map.metadata.xSize-1, cameraR.toInt+1)
    val tileT = math.max(0, cameraT.toInt)
    val tileB = math.min(map.metadata.ySize-1, cameraB.toInt+1)
    /*println("Render")
    println(tileL)
    println(tileR)
    println(tileT)
    println(tileB)*/
    for(layerAry <- List(
        mapData.botLayer, mapData.midLayer, mapData.topLayer)) {
      for(tileY <- tileT to tileB) {
        for(tileX <- tileL to tileR) {
          val idx = map.metadata.idx(tileX, tileY)
          val byte1 = layerAry(idx)
          val byte2 = layerAry(idx+1)
          val byte3 = layerAry(idx+2)
          
          if(byte1 < 0) {
            if(byte1 == RpgMap.autotileByte) { // Autotile
              // TODO
              //println("Draw autotile")
            }
          } else { // Regular tile
            //println("Draw regular tile")
            val tex = tilesetTexs(byte1)
            batch.draw(
                tex,
                tileX.toFloat, tileY.toFloat,
                1.0f, 1.0f,
                byte2*Tileset.tilesize, byte3*Tileset.tilesize,
                Tileset.tilesize, Tileset.tilesize,
                false, true)
            
          }
        }
      }
    }
    
    
    batch.end()
  }
  override def dispose() {}
  override def pause() {}
  override def resume() {}
  override def resize(x: Int, y: Int) {}
}
