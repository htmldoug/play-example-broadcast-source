package testkit

import java.net.ServerSocket

object TestPort {

  /**
    * @return a free TCP port
    */
  def apply(): Int = {
    val socket = new ServerSocket(0)
    val port = socket.getLocalPort
    socket.close()
    port
  }
}
