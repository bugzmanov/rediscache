package com.bugzmanov

import java.io.File

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Sink, Source}
import akka.util.ByteString
import akka.stream.scaladsl._
import redis.clients.jedis.Jedis

import scala.concurrent.Future
import scala.util.{Random, Try}

//todo: remove
object LoadGenerator {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val dispatcher = system.dispatcher

  def runRequest(req: HttpRequest): Future[Done] =
    Http()
      .singleRequest(req)
      .flatMap { response =>
//        response.entity.dataBytes.runWith(Sink.ignore)
        response.entity.discardBytes(materializer).future()
      }


//  def responseToByteSource[T](in: (HttpResponse, T)): Source[ByteString, Any] = in match {
//    case (response, _) => response.entity.dataBytes
//  }

//  def responseToByteSource[T](in: (HttpResponse, T)): Source[ByteString, Any] = in match {
//    case (response, _) => response.entity.discardBytes(materializer).future()
//  }

  def responseToByteSource[T](in: (HttpResponse, T)): Source[Done, Any] = in match {
    case (response, _) => Source.future(response.entity.discardBytes(materializer).future())
  }

  def downloadViaFlow(uri: Uri): Future[Done] = {
    val request = HttpRequest(uri = "http://localhost:8080/v1/blob")
    val source = Source.cycle(() =>
      (1 to 120).map(i => (HttpRequest(uri =s"http://localhost:8080/v1/$i"),())).iterator)
    val requestResponseFlow = Http().superPool[Unit](settings = ConnectionPoolSettings(system).withMaxConnections(20))

    source.
      async.
      via(requestResponseFlow).
      map(responseOrFail).
      flatMapConcat(responseToByteSource).
      runWith(Sink.ignore)
  }

  def responseOrFail[T](in: (Try[HttpResponse], T)): (HttpResponse, T) = in match {
    case (responseTry, context) => (responseTry.get, context)
  }

  def main(args: Array[String]): Unit = {
    val poolClientFlow = Http().cachedHostConnectionPool[Int]("localhost", port = 8080)
    new Thread(() => {
      var round: Long = 1;
      val jedis = new Jedis("localhost", 6379)

//      val bytes = Array.ofDim[Byte](1024 * 1024 * 500)
//      jedis.set("large1".getBytes(), bytes);
//      jedis.set("large2".getBytes(), bytes);
//      jedis.set("large3".getBytes(), bytes);
//      jedis.set("large4".getBytes(), bytes);
//      jedis.set("large5".getBytes(), bytes);

        for(i <- 1 to 20) {
          jedis.set(("small_" +i.toString).getBytes, Random.nextString(1024).getBytes)
          jedis.set(("medium_" +i.toString).getBytes, Random.nextString(1024*1024).getBytes)
        }
        round+=1;
    }).start();

//    downloadViaFlow("")
//    val ololo = Source.single(HttpRequest(uri = "/v1/blob"))
//       .via(poolClientFlow)
//       .runWith(Sink.ignore)


  }
}
