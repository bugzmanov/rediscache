package com.bugzmanov.database

import java.net.URI

import com.bugzmanov.cache.Cache
import com.bugzmanov.scalautils.Managed.withResources
import redis.clients.jedis.{JedisPool, JedisPoolConfig}

import scala.concurrent.duration.Duration

trait RedisClient {
  def get(key: String): Option[Array[Byte]]
}

class JedisRedisClient(host: String,
                       port: Int,
                       connectionTimeout: Duration,
                       soTimeout: Duration,
                       poolSize: Int) extends RedisClient {

  private val poolConfig = {
    val config = new JedisPoolConfig()
    config.setMaxTotal(poolSize)
    config
  }

  private val pool = new JedisPool(
    poolConfig,
    new URI(s"redis://$host:$port/"),
    connectionTimeout.toMillis.intValue(),
    soTimeout.toMillis.intValue())

  def get(key: String): Option[Array[Byte]] = {
    withResources(pool.getResource) { jedis =>
      Option(jedis.get(key.getBytes))
    }
  }
}

class CachingRedisClient(delegate: RedisClient, cache: Cache) extends RedisClient {

  override def get(key: String): Option[Array[Byte]] = {
    val maybeCache = cache.get(key.getBytes())

    maybeCache match {
      case None =>
        for (data <- delegate.get(key)) yield {
          cache.put(key.getBytes(), data)
          data
        }
      case _ => maybeCache
    }
  }
}
