package com.bugzmanov

import com.bugzmanov.cache.GuavaBasedCache
import com.bugzmanov.metrics.{Intrumentation, Metrics}
import com.bugzmanov.database.{CachingRedisClient, JedisRedisClient}
import com.typesafe.config.Config

import scala.concurrent.duration._

class ApplicationContext(config: Config) {

  val metrics = new Metrics(config.getString("rediscache-app.metrics.graphite-host"),
    config.getInt("rediscache-app.metrics.graphite-port"))

  val metricsInstrumentation = new Intrumentation(metrics.registry)

  private val guavaBasedCache = new GuavaBasedCache(1000,
    config.getInt("rediscache-app.cache.capacity"),
    config.getDuration("rediscache-app.cache.expiry"),
    metricsInstrumentation.instrument,
    metricsInstrumentation.instrument[String, Array[Byte]]
  )

  val redis = new CachingRedisClient(
    metricsInstrumentation.instrument(
      new JedisRedisClient(
        config.getString("rediscache-app.redis.host"),
        config.getInt("rediscache-app.redis.port"),
        config.getDuration("rediscache-app.redis.connection-timeout").toMillis.millis,
        config.getDuration("rediscache-app.redis.reading-timeout").toMillis.millis,
        config.getInt("rediscache-app.redis.connections-pool-size")
      )
    ),
    guavaBasedCache
  )

  val dataSource = new DataSource(redis)

  metricsInstrumentation.instrumentJVM()
}
