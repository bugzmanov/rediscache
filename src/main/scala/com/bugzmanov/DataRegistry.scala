package com.bugzmanov

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}



//todo: remove
object DataRegistry {
  sealed trait Command
  final case class GetDataRequest(key: String, replyTo: ActorRef[GetDataResponse]) extends Command

  final case class GetDataResponse(maybeData: Option[Array[Byte]])
  final case class ActionPerformed(description: String)

  def apply(): Behavior[Command] = registry()

  private def registry(): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetDataRequest(key, replyTo) =>
        val bytes = Option(Array[Byte](0))
        replyTo ! GetDataResponse(bytes)
        Behaviors.same
    }
}
