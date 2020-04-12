package com.bugzmanov

import java.net.ServerSocket

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}
import redis.clients.jedis.Jedis
import redis.embedded.RedisServer

import scala.concurrent.duration.DurationInt
import scala.util.Random

class WebServerRoutesSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest with BeforeAndAfter {


  private lazy val redisPort: Int = PortFinder.getFreePort
  private lazy val redisServer = new RedisServer(redisPort)

  // the Akka HTTP route testkit does not yet support a typed actor system (https://github.com/akka/akka-http/issues/2036)
  // so we have to adapt for now
  lazy val testKit = ActorTestKit(ConfigFactory.load("application-test").
    withValue("rediscache-app.redis.port", ConfigValueFactory.fromAnyRef(redisPort)))
  implicit def typedSystem = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.toClassic

  val userRegistry = testKit.spawn(DataRegistry())
  val applicationContext = new ApplicationContext(testKit.config)

  lazy val routes = new WebServerRoutes(userRegistry, applicationContext.dataSource).routes

  implicit val timeout = RouteTestTimeout(50.seconds)

  "WebServerRoutes" should {
    "return no data if key not present " in {
      val request = HttpRequest(uri = "/v1/random-key" + Random.nextInt(10000))

      request ~> Route.seal(routes) ~> check {
        status should ===(StatusCodes.NotFound)
      }
    }

    "return saved data from redis" in {
      val jedis = new Jedis("localhost", redisPort)

      val (key, value) = ("key" + Random.nextInt(100000), "value" + Random.nextString(100))
      jedis.set(key, value)

      val request = HttpRequest(uri = "/v1/" + key)

      request ~> Route.seal(routes) ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/octet-stream`)

        entityAs[String] should ===(value)
      }
    }

    "return cached value on next request" in {
      val jedis = new Jedis("localhost", redisPort)

      val (key, value) = ("key" + Random.nextInt(100000), "value" + Random.nextString(100))
      jedis.set(key, value)

      val request = HttpRequest(uri = "/v1/" + key)

      request ~> Route.seal(routes) ~> check {
        status should ===(StatusCodes.OK)
        entityAs[String] should ===(value)
      }

      jedis.set(key.getBytes, "should be ignored".getBytes)

      request ~> Route.seal(routes) ~> check {
        status should ===(StatusCodes.OK)
        entityAs[String] should ===(value)
      }

    }

    "return 500 if backing server is not running" in {
      redisServer.stop()
      val request = HttpRequest(uri = "/v1/random-key" + Random.nextInt(100))

      request ~> Route.seal(routes) ~> check {
        status should ===(StatusCodes.InternalServerError)
      }
    }

  }

  override def beforeAll {
    redisServer.start()
  }

  override def afterAll {
    redisServer.stop()
  }

}
