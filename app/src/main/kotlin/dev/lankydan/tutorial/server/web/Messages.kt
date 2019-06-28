package dev.lankydan.tutorial.server.web

import dev.lankydan.tutorial.flows.SendMessageFlow
import dev.lankydan.tutorial.states.MessageState
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.utilities.getOrThrow
import java.util.*

data class Message(val recipient: String, val contents: String)

fun Routing.messages(proxy: CordaRPCOps) {
  route("/messages") {
    get("/") {
      call.respond(HttpStatusCode.OK, proxy.vaultQueryBy<MessageState>().states.map { it.state.data })
    }
    post("/") {
      val received = call.receive<Message>()
      UUID.randomUUID().let {
        try {
          val message = proxy.startFlow(
            ::SendMessageFlow,
            state(proxy, received, it)
          ).returnValue.getOrThrow().coreTransaction.outputStates.first() as MessageState
          call.respond(HttpStatusCode.Created, message)
        } catch (e: Exception) {
          call.respond(HttpStatusCode.InternalServerError, e.message ?: "Something went wrong")
        }
      }
    }
  }
}

private fun state(proxy: CordaRPCOps, message: Message, id: UUID) =
  MessageState(
    sender = proxy.nodeInfo().legalIdentities.first(),
    recipient = parse(proxy, message.recipient),
    contents = message.contents,
    linearId = UniqueIdentifier(id.toString())
  )

private fun parse(proxy: CordaRPCOps, party: String) =
  proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(party))
    ?: throw IllegalArgumentException("Unknown party name.")