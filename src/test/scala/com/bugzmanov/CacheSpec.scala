package com.bugzmanov

import java.time.Duration
import java.util

import com.bugzmanov.cache.GuavaBasedCache
import org.scalatest.concurrent.Eventually
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._
import scala.util.Random

class CacheSpec extends FlatSpec with Matchers with Eventually {

  "A Cache" should "remember saved values" in {
    val cache = new GuavaBasedCache(1000, 100, Duration.ofSeconds(1))

    cache.put(Array(0,1,2), Array(6,6,6))
    cache.get(Array(0,1,2)).get should be (Array(6,6,6))
  }

  "A Cache" should "evict item after 'expiry' period" in {
    val cache = new GuavaBasedCache(1000, 100, Duration.ofSeconds(1))

    cache.put(Array(0,1,2), Array(6,6,6))
    cache.get(Array(0,1,2)).get should be (Array(6,6,6))

    eventually(timeout(2.seconds)) {
      cache.get(Array(0,1,2)) should be (None)
    }
  }

  "A Cache" should "evict LRU after hitting capacity limit" in {
    val cache = new GuavaBasedCache(1000, 2, Duration.ofSeconds(100))

    cache.put(Array(1,1,1), Array(1,1,1))
    cache.put(Array(2,2,2), Array(6,6,6))

    cache.get(Array(1,1,1))

    cache.put(Array(3,3,3), Array(3,3,3))

    cache.get(Array(2,2,2)) should be (None)
    cache.get(Array(1,1,1)).get should be (Array(1,1,1))
    cache.get(Array(3,3,3)).get should be (Array(3,3,3))
  }

  val cache = new GuavaBasedCache(1000, 30000, Duration.ofSeconds(1000))

//  "olo" should "ldld" in {
//    System.out.println(Runtime.getRuntime.maxMemory());
//
//    Runtime.getRuntime.gc()
//    Runtime.getRuntime.gc()
//    val before = Runtime.getRuntime.totalMemory() - Runtime.getRuntime.freeMemory()
//
//    for(i <- 1 to 2000) {
//      cache.put(Random.nextBytes(1024 * 100), Random.nextBytes(1024 * 1024))
//    }
//
//    Runtime.getRuntime.gc()
//    Runtime.getRuntime.gc()
//    Runtime.getRuntime.gc()
//    val after = Runtime.getRuntime.totalMemory()  - Runtime.getRuntime.freeMemory()
//
//    System.out.println((after - before) / 1024.0)
//    System.out.println(cache.cache.size())
//  }

}
