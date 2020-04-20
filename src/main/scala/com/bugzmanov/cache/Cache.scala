package com.bugzmanov.cache

import java.time.Duration
import java.util.concurrent.{ConcurrentHashMap, TimeUnit}
import java.util.concurrent.atomic.AtomicLong

import com.google.common.cache.{CacheBuilder, RemovalNotification}
import com.google.common.cache.{Cache => Guava}


trait Cache {
  def get(key: Array[Byte]): Option[Array[Byte]]

  def put(key: Array[Byte], value: Array[Byte])
}


class CacheSize {
  val bytes = new AtomicLong()
  val map = new ConcurrentHashMap[String, Integer]()

  def add(key: String, value: Integer): Long = {
    while(map.putIfAbsent(key, value) != null) { /* spin while remove is cleaning up */ }
    bytes.addAndGet(key.getBytes().length + value)
  }

  def remove(key:String): Unit = {
    val removed = map.remove(key)
    bytes.addAndGet(-(key.getBytes().length + removed))
  }

  def estimateSize(): Long = bytes.get()
}

class GuavaBasedCache(totalSizeMB: Int,
                      elementsCount: Int,
                      expiry: Duration,
                      instrument: CacheSize => CacheSize  = identity,
                      instrumentGuava: Guava[String, Array[Byte]] => Guava[String, Array[Byte]]  = identity) extends Cache {

  private val cacheSize: CacheSize = instrument(new CacheSize)
   private val cache = instrumentGuava(CacheBuilder.newBuilder()
    .maximumSize(elementsCount)
    .removalListener((notification: RemovalNotification[String, Array[Byte]]) => {
      cacheSize.remove(notification.getKey)
    })
    .softValues()
    .recordStats()
    .expireAfterWrite(expiry.getSeconds, TimeUnit.SECONDS)
    .build[String, Array[Byte]]())

  override def get(key: Array[Byte]): Option[Array[Byte]] = {
    Option(cache.getIfPresent(new String(key)))
  }

  override def put(key: Array[Byte], value: Array[Byte]): Unit = {
    if(cache.asMap().putIfAbsent(new String(key), value) == null) {
      cacheSize.add(new String(key),value.length)
    }
  }
}


