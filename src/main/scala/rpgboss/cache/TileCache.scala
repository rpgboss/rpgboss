package rpgboss.cache

import rpgboss.model._

import java.awt.image._
import com.google.common.cache._

class TileCache(proj: Project, map: RpgMap, cacheMaxSize: Int) {
  val autotiles = proj.autotiles.map(Autotile.readFromDisk(proj, _))
  val tilesets  = map.tilesets.map(Tileset.readFromDisk(proj, _))
  
  val cache = CacheBuilder.newBuilder()
    .concurrencyLevel(1)
    .softValues()
    .maximumSize(cacheMaxSize)
    .expireAfterWrite(10, java.util.concurrent.TimeUnit.MINUTES)
    .build(new CacheLoader[Integer, BufferedImage] {
      def load(tilecode: Integer) = {
        val tilesetIdx = (tilecode >>> 24).asInstanceOf[Byte]
        val secondByte = (tilecode >>> 16)&0xff
        val thirdByte = (tilecode >>> 8)&0xff
        val frame = tilecode.asInstanceOf[Byte] // just the last bits
        
        if(tilesetIdx == RpgMap.autotileByte) {
          // Autotile
          val autotileNum = secondByte
          val autotileConfig = thirdByte
          
          if(autotiles.length > autotileNum) {
            autotiles(autotileNum).getTile(autotileConfig, frame)
          } else ImageResource.errorTile
          
        } else if(tilesetIdx >= 0) {
          // Regular tile
          val x = secondByte
          val y = thirdByte
          
          if(tilesets.length > tilesetIdx) {
            tilesets(tilesetIdx).getTile(x, y)
            
          } else ImageResource.errorTile
        } else ImageResource.errorTile
      }
    })
  
  // frame here means the animation frame
  def getTileImage(mapData: Array[Byte], bi: Int, frame: Byte = 0) = {
    val tilecode : Int = 
      ((mapData(bi)&0xff) << 24) |
      ((mapData(bi+1)&0xff) << 16) |
      ((mapData(bi+2)&0xff) << 8) |
      ((frame&0xff))
    
    cache.get(new Integer(tilecode))
  }
}
