package dev.lankydan.tutorial.server.rpc

import net.corda.client.rpc.CordaRPCClient
import net.corda.client.rpc.CordaRPCConnection
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.NetworkHostAndPort
import javax.annotation.PreDestroy

fun connectToNode(
  host: String,
  rpcPort: Int,
  username: String,
  password: String
): CordaRPCOps {
  val rpcAddress = NetworkHostAndPort(host, rpcPort)
  val rpcClient = CordaRPCClient(rpcAddress)
  val rpcConnection = rpcClient.start(username, password)
  return rpcConnection.proxy
}