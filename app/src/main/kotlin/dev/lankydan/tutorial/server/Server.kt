package dev.lankydan.tutorial.server

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
import dev.lankydan.tutorial.server.rpc.connectToNode
import dev.lankydan.tutorial.server.web.messages
import io.ktor.application.Application
import io.ktor.application.ApplicationStopped
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.jackson.JacksonConverter
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import net.corda.client.jackson.JacksonSupport
import net.corda.client.rpc.CordaRPCConnection
import net.corda.core.messaging.CordaRPCOps
import org.slf4j.event.Level
import java.util.concurrent.TimeUnit

fun main() {
  embeddedServer(
    Netty,
    port = System.getProperty("server.port").toInt(),
    module = Application::module
  ).start().addShutdownHook()
}

fun Application.module() {
  val connection: CordaRPCConnection = connectToNode()
  install(CallLogging) { level = Level.INFO }
  install(ContentNegotiation) { cordaJackson(connection.proxy) }
  routing { messages(connection.proxy) }
  addShutdownEvent(connection)
}

fun ContentNegotiation.Configuration.cordaJackson(proxy: CordaRPCOps) {
  val mapper: ObjectMapper = JacksonSupport.createDefaultMapper(proxy)
  mapper.apply {
    setDefaultPrettyPrinter(DefaultPrettyPrinter().apply {
      indentArraysWith(DefaultPrettyPrinter.FixedSpaceIndenter.instance)
      indentObjectsWith(DefaultIndenter("  ", "\n"))
    })
  }
  val converter = JacksonConverter(mapper)
  register(ContentType.Application.Json, converter)
}

fun NettyApplicationEngine.addShutdownHook() {
  Runtime.getRuntime().addShutdownHook(Thread {
    stop(1, 1, TimeUnit.SECONDS)
  })
  Thread.currentThread().join()
}

fun Application.addShutdownEvent(connection: CordaRPCConnection) {
  environment.monitor.subscribe(ApplicationStopped) {
    println("Time to clean up")
    // not hit when run through intellij
    connection.notifyServerAndClose()
  }
}