package com.example.link.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

class GuardianRepository(
    private val baseUrl: String,
    private val appSecret: String,
    private val installId: String,
) {
    private val client = HttpClient(OkHttp) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 45_000
        }
    }

    suspend fun sendMessage(message: String): AssistantReply =
        try {
            client.post("${baseUrl.trimEnd('/')}/app/message") {
                header("X-App-Secret", appSecret)
                contentType(ContentType.Application.Json)
                setBody(AppMessageRequest(installId, message))
            }.body()
        } catch (e: CancellationException) {
            throw e
        } catch (e: ClientRequestException) {
            val code = e.response.status.value
            if (code == 401) {
                AssistantReply(ReplyType.ERROR, "La clave de conexión no es válida.")
            } else {
                AssistantReply(ReplyType.ERROR, "El servidor rechazó la solicitud ($code).")
            }
        } catch (e: Exception) {
            AssistantReply(ReplyType.ERROR, "No pude conectar con el asistente. Revisa la conexión.")
        }
}