package com.bugzmanov.metrics

import com.bugzmanov.cache.CacheSize
import com.bugzmanov.database.RedisClient
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.util.HierarchicalNameMapper
import io.micrometer.core.instrument.{Clock, Gauge, MeterRegistry}
import io.micrometer.elastic.{ElasticConfig, ElasticMeterRegistry}
import io.micrometer.graphite.{GraphiteConfig, GraphiteMeterRegistry}

class Metrics (backendhost: String, port: Int) {

  private val graphiteConfig = new GraphiteConfig() {
    override def host() :String = backendhost

    override def get(k: String): String = {
      k match {
        case "graphite.port" => port.toString
        case "graphite.protocol" =>  "PLAINTEXT"
        case "graphite.step" => "PT10S"
        case _ => null //defaults
      }
    }
  }

//  val registry: MeterRegistry = new GraphiteMeterRegistry(
//    graphiteConfig, Clock.SYSTEM,
//    HierarchicalNameMapper.DEFAULT
//  )

  import io.micrometer.core.instrument.Clock
  import io.micrometer.core.instrument.MeterRegistry

  val elasticConfig: ElasticConfig = new ElasticConfig() {
    override def host() :String = backendhost

    override def get(k: String): String = {
      k match {
        case "elastic.port" => port.toString
        case "elastic.step" => "PT10S"
        case "elastic.indexDateFormat" => "yyyy-MM-dd"
        case _ => null //defaults
      }
    }
  }
  val registry: MeterRegistry = new ElasticMeterRegistry(elasticConfig, Clock.SYSTEM)
}

class Intrumentation(registry: MeterRegistry) {

  def instrumentJVM() = {
    new JvmMemoryMetrics().bindTo(registry)
    new ProcessorMetrics().bindTo(registry)
  }

  def instrument(cacheSize: CacheSize): CacheSize = registry.gauge(
    "cache.size.estimate.bytes",
    cacheSize,
    (c: CacheSize) => c.bytes.get)

  def instrument[K,V](guavaCache: com.google.common.cache.Cache[K,V]) = {
    Gauge.builder("cache.size.items", () => {
      guavaCache.cleanUp() // todo: this is quite expensive
      guavaCache.size()}).register(registry)
    Gauge.builder("cache.stats.hit_rate", () => guavaCache.stats().hitRate() * 100).register(registry)
    Gauge.builder("cache.stats.miss_rate", () => guavaCache.stats().missRate() * 100).register(registry)
    guavaCache
  }

  def instrument(redisClient: RedisClient): RedisClient = new InstrumentedRedisClient(redisClient, registry)

  class InstrumentedRedisClient(delegate: RedisClient, metrics: MeterRegistry) extends RedisClient {
    private val errorsCount = metrics.counter("redis_client.errors");
    private val successCount =  metrics.counter("redis_client.success");
    private val time = metrics.timer("redis_client.timer");

    override def get(key: String): Option[Array[Byte]] = {

      try {
        val result = time.record( () =>
          delegate.get(key)
        )
        successCount.increment()
        result
      } catch {
        case  e: Exception =>
          errorsCount.increment()
          throw e
      }
    }
  }
}
