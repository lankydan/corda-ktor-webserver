package dev.lankydan.tutorial.server.rpc

import net.corda.client.rpc.CordaRPCClient
import net.corda.client.rpc.CordaRPCConnection
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.NetworkHostAndPort
import javax.annotation.PreDestroy

// not used, just wanted to keep for reference
class NodeRPCConnection(
  host: String,
  rpcPort: Int,
  username: String,
  password: String
) /*: AutoCloseable*/ {

  private val rpcConnection: CordaRPCConnection
  val proxy: CordaRPCOps

  init {
    val rpcAddress = NetworkHostAndPort(host, rpcPort)
    val rpcClient = CordaRPCClient(rpcAddress)
    rpcConnection = rpcClient.start(username, password)
    proxy = rpcConnection.proxy
  }

  // need to find kodein equivalent for this
//  @PreDestroy
//  override fun close() {
//    rpcConnection.notifyServerAndClose()
//  }
}