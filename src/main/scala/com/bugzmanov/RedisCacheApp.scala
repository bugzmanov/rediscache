package com.bugzmanov

import java.util.concurrent.TimeUnit

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.ServerSettings
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsRoute._
import fr.davit.akka.http.metrics.dropwizard.DropwizardRegistry
import io.dropwizard.metrics5.graphite.{Graphite, GraphiteReporter}
import io.dropwizard.metrics5.{MetricFilter, MetricRegistry}

import scala.util.{Failure, Success}

object RedisCacheApp {

  private def startHttpServer(routes: Route, system: ActorSystem[_]): Unit = {
    implicit val classicSystem: akka.actor.ActorSystem = system.toClassic
    import system.executionContext

    val config = system.settings.config
    val port = config.getInt("rediscache-app.webserver.bind-port")
    val host = config.getString("rediscache-app.webserver.bind-host")
    val maxConnections = config.getInt("rediscache-app.webserver.max-connections")

    val settings = ServerSettings(config).withMaxConnections(maxConnections)


    val dropwizard: MetricRegistry = new MetricRegistry();

    val registry = DropwizardRegistry(dropwizard)


    val graphite = new Graphite(config.getString("rediscache-app.metrics.graphite-host"),
          config.getInt("rediscache-app.metrics.graphite-port"))

    val reporter = GraphiteReporter.forRegistry(dropwizard)
      .prefixedWith("akka-http").
      convertRatesTo(TimeUnit.SECONDS).
      convertDurationsTo(TimeUnit.MILLISECONDS).
      filter(MetricFilter.ALL).build(graphite)

    reporter.start(10, TimeUnit.SECONDS)

    val futureBinding = Http().bindAndHandle(routes.recordMetrics(registry), host, port, settings = settings)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)

      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }

  def main(args: Array[String]): Unit = {
    System.out.println(Runtime.getRuntime.maxMemory / 1024.0 / 1024.0)

    val rootBehavior = Behaviors.setup[Nothing] { context =>
      val application = new ApplicationContext(context.system.settings.config)

      val userRegistryActor = context.spawn(DataRegistry(), "UserRegistryActor")
      context.watch(userRegistryActor)

      val routes = new WebServerRoutes(userRegistryActor, application.dataSource)(context.system)
      startHttpServer(routes.routes, context.system)

      Behaviors.empty
    }
    val system = ActorSystem[Nothing](rootBehavior, "HelloAkkaHttpServer")
  }
}
