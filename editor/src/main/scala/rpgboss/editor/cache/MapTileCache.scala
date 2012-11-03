package rpgboss.editor.cache

import rpgboss.model._
import rpgboss.model.resource._

import java.awt.image._
import com.google.common.cache._

/**
 * A tile cache applicable to only one map.
 */
class MapTileCache(
    assetCache: AssetCache, 
    map: RpgMap, 
    cacheMaxSize: Int = 5000) 
{
  val tilesets = map.metadata.tilesets.map(assetCache.tilesetMap.get(_).get) 
  val autotiles = map.metadata.autotiles.map(assetCache.autotileMap.get(_).get)
  
  val cache = CacheBuilder.newBuilder()
    .concurrencyLevel(1)
    .softValues()
    .maximumSize(cacheMaxSize)
    .expireAfterWrite(10, java.util.concurrent.TimeUnit.MINUTES)
    .build(new CacheLoader[(Byte, Byte, Byte, Byte), BufferedImage] {
      def load(tileTuple: (Byte, Byte, Byte, Byte)) = loadTile(tileTuple)
    })  
  
  def loadTile(tileTuple: (Byte, Byte, Byte, Byte)) = {
    val (tilesetIdx, secondByte, thirdByte, frame) = tileTuple
    val secondBytePos = secondByte & 0xff
    val thirdBytePos = thirdByte & 0xff
    
    if(tilesetIdx == RpgMap.autotileByte) {
      // Autotile
      val autotileNum = secondBytePos
      val autotileConfig = thirdBytePos
      
      if(autotiles.length > autotileNum) {
        autotiles(autotileNum).getTileImage(autotileConfig, frame)
      } else ImageResource.errorTile
      
    } else if(tilesetIdx >= 0) {
      // Regular tile
      val x = secondBytePos
      val y = thirdBytePos
      
      if(tilesets.length > tilesetIdx) {
        tilesets(tilesetIdx).getTileImage(x, y)
        
      } else ImageResource.errorTile
    } else ImageResource.errorTile
  }
  
  // frame here means the animation frame
  def getTileImage(
      mapData: Array[Array[Byte]], xTile: Int, yTile: Int, frame: Byte = 0) = {
    
    val row = mapData(yTile)
    val idx = xTile*RpgMap.bytesPerTile
    val tileTuple = (row(idx), row(idx+1), row(idx+2), frame) 
    cache.get(tileTuple)
    //loadTile(tileTuple)
  }
}
