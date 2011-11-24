package rpgboss.cache

import rpgboss.model._

import java.awt.image._
import com.google.common.cache._

class TileCache(proj: Project, autotiles: Vector[Autotile], 
                map: RpgMap, cacheMaxSize: Int = 5000) {
  val tilesets  = map.metadata.tilesets.map(Tileset.readFromDisk(proj, _))
  
  def loadTile(tileTuple: (Byte, Byte, Byte, Byte)) = {
    val (tilesetIdx, secondByte, thirdByte, frame) = tileTuple
    val secondBytePos = secondByte & 0xff
    val thirdBytePos = thirdByte & 0xff
    
    if(tilesetIdx == RpgMap.autotileByte) {
      // Autotile
      val autotileNum = secondBytePos
      val autotileConfig = thirdBytePos
      
      if(autotiles.length > autotileNum) {
        autotiles(autotileNum).getTile(autotileConfig, frame)
      } else ImageResource.errorTile
      
    } else if(tilesetIdx >= 0) {
      // Regular tile
      val x = secondBytePos
      val y = thirdBytePos
      
      if(tilesets.length > tilesetIdx) {
        tilesets(tilesetIdx).getTile(x, y)
        
      } else ImageResource.errorTile
    } else ImageResource.errorTile
  }
  
  val cache = CacheBuilder.newBuilder()
    .concurrencyLevel(1)
    .softValues()
    .maximumSize(cacheMaxSize)
    .expireAfterWrite(10, java.util.concurrent.TimeUnit.MINUTES)
    .build(new CacheLoader[(Byte, Byte, Byte, Byte), BufferedImage] {
      def load(tileTuple: (Byte, Byte, Byte, Byte)) = loadTile(tileTuple)
    })
    
  
  // frame here means the animation frame
  def getTileImage(mapData: Array[Byte], bi: Int, frame: Byte = 0) = {
    val tileTuple = (mapData(bi), mapData(bi+1), mapData(bi+2), frame) 
    cache.get(tileTuple)
    //loadTile(tileTuple)
  }
}
