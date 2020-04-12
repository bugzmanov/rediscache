package com.bugzmanov
import java.io.IOException
import java.net.ServerSocket

object PortFinder {
  def getFreePort: Int = {

    var socket: ServerSocket = null
    try {
      socket = new ServerSocket(0)
      socket.setReuseAddress(true)
      return socket.getLocalPort
    } catch {
      case e: IOException => //ignore
    } finally {
      if (socket != null) {
        try {
          socket.close()
        } catch {
          case e: IOException =>
        }
      }
    }
    throw new IllegalStateException("Could not find a free TCP/IP port")
  }
}
