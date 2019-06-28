package dev.lankydan.tutorial.server

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import dev.lankydan.tutorial.server.rpc.NodeRPCConnection
import dev.lankydan.tutorial.server.rpc.connectToNode
import dev.lankydan.tutorial.server.web.messages
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.jackson.JacksonConverter
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import net.corda.client.jackson.JacksonSupport
import net.corda.core.messaging.CordaRPCOps
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

fun main(args: Array<String>) {
  val kodein = Kodein {
    bind<CordaRPCOps>() with singleton {
      // proxy defined for clarity
      val proxy: CordaRPCOps = connectToNode(
        System.getenv("config.rpc.host"),
        System.getenv("config.rpc.port").toInt(),
        System.getenv("config.rpc.username"),
        System.getenv("config.rpc.password")
      )
      proxy
    }
  }
  embeddedServer(Netty, port = System.getenv("server.port").toInt()) {
    install(ContentNegotiation) {
      val proxy by kodein.instance<CordaRPCOps>()
      cordaJackson(proxy)
    }
    main(kodein)
  }.start()
}

fun Application.main(kodein: Kodein) {
  val proxy by kodein.instance<CordaRPCOps>()
  install(CallLogging)
  routing {
    messages(proxy)
  }
}

fun ContentNegotiation.Configuration.cordaJackson(proxy: CordaRPCOps) {
  val mapper = JacksonSupport.createDefaultMapper(proxy)
  mapper.apply {
    setDefaultPrettyPrinter(DefaultPrettyPrinter().apply {
      indentArraysWith(DefaultPrettyPrinter.FixedSpaceIndenter.instance)
      indentObjectsWith(DefaultIndenter("  ", "\n"))
    })
  }
  val converter = JacksonConverter(mapper)
  register(ContentType.Application.Json, converter)
}