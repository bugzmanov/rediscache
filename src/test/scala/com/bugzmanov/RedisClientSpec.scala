package com.bugzmanov

import java.net.ServerSocket
import java.time.Duration

import com.bugzmanov.cache.Cache
import com.bugzmanov.database.{CachingRedisClient, JedisRedisClient, RedisClient}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpec, Matchers, WordSpec}
import org.scalatest.concurrent.Eventually
import redis.clients.jedis.Jedis
import redis.embedded.RedisServer

import scala.concurrent.duration._
import scala.util.Random

class RedisClientSpec extends WordSpec  with Matchers with Eventually with BeforeAndAfterAll {
  private lazy val redisPort: Int = PortFinder.getFreePort
  private lazy val redisServer = new RedisServer(redisPort)
  private lazy val redisClient = new JedisRedisClient("localhost", redisPort,
    Duration.ofMillis(100).toMillis.millis,
    Duration.ofMillis(100).toMillis.millis,
    1);

  "RedisClient" should {
    "return data from redis if exists" in {
      val jedis = new Jedis("localhost", redisPort)
      val value = Random.nextString(100)
      val key = "ololo" + Random.nextInt(10)
      jedis.set(key, value)

      redisClient.get(key).get should be (value.getBytes)

    }

    "return None if not data found" in {
      val key = "notololo" + Random.nextInt(10)

      redisClient.get(key) should be (None)
    }
  }

  override def beforeAll {
    redisServer.start()
  }

  override def afterAll {
    redisServer.stop()
  }
}

class CachingRedisClientSpec extends WordSpec  with Matchers with Eventually with BeforeAndAfterAll {
  "RedisClient" should {
    "not call delegate if data exists in cache" in {
      val cachedData = Array[Byte](0, 1, 2)

      val cache = new Cache {
        override def get(key: Array[Byte]): Option[Array[Byte]] = Some(cachedData)
        override def put(key: Array[Byte], value: Array[Byte]): Unit = {}
      }

      val delegate = new RedisClient {
        override def get(key: String): Option[Array[Byte]] = fail("delegate shouldn't be called")
      }

      val client = new CachingRedisClient(delegate, cache)

      client.get("any-key").get should be(cachedData)
    }

    "should call delegate if data doesn't exists in cache and cache result" in {
      val data = Array[Byte](0, 1, 2)

      var map = Map[String, Array[Byte]]()
      val cache = new Cache {
        override def get(key: Array[Byte]): Option[Array[Byte]] = map.get(new String(key))
        override def put(key: Array[Byte], value: Array[Byte]): Unit = {
          map = map + (new String(key) -> value)
        }
      }

      val delegate = new RedisClient {
        override def get(key: String): Option[Array[Byte]] = Some(data)
      }

      val client = new CachingRedisClient(delegate, cache)

      client.get("any-key").get should be(data)
      map("any-key") should be (data)
    }

  }
}
