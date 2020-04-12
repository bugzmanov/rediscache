package com.bugzmanov

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout

class WebServerRoutes(userRegistry: ActorRef[DataRegistry.Command], dataSource: DataSource)(implicit val system: ActorSystem[_]) {

  private implicit val timeout = Timeout.create(system.settings.config.getDuration("rediscache-app.routes.ask-timeout"))

  val routes: Route =
    concat(
      path("v1" / Remaining) { name =>
        val maybeData = dataSource.get(name)
        maybeData match {
          case None =>
            complete((NotFound, ""))
          case Some(data) =>
            complete(HttpEntity(ContentTypes.`application/octet-stream`, data))
        }
      })
}
