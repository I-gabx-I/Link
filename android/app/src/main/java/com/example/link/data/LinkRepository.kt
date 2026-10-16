package com.example.link.data

import android.content.Context
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class LinkContact(val name: String, val username: String, val aliases: List<String> = emptyList())

data class LinkResponse(
    val type: String,
    val reply: String,
    val summary: String = "",
    val actionUrl: String? = null,
    val contacts: List<LinkContact>? = null,
) {
    val failed: Boolean get() = type == "error" || type == "denied"
}

class LinkPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("link_demo", Context.MODE_PRIVATE)
    val installId: String = preferences.getString("install_id", null) ?: UUID.randomUUID().toString().also {
        preferences.edit().putString("install_id", it).commit()
    }
    val gatewayUrl: String get() = preferences.getString("gateway_url", "http://10.0.2.2:8000")!!
    val appSecret: String get() = preferences.getString("app_secret", "")!!
    val configured: Boolean get() = gatewayUrl.isNotBlank() && appSecret.isNotBlank()

    fun saveConnection(url: String, secret: String) {
        val parsed = URL(url.trim())
        require(parsed.protocol == "https" || parsed.protocol == "http") { "Usa una dirección HTTP o HTTPS." }
        require(parsed.host.isNotBlank() && parsed.userInfo == null && parsed.query == null && parsed.ref == null && parsed.path.trim('/').isEmpty()) { "Usa solo la dirección del backend, sin rutas: http://127.0.0.1:8000 para USB." }
        require(secret.trim().isNotBlank() && secret.trim().all { it.code in 33..126 }) { "Escribe la clave de conexión sin espacios." }
        preferences.edit().putString("gateway_url", url.trim().trimEnd('/'))
            .putString("app_secret", secret.trim()).apply()
    }

    fun cacheContacts(contacts: List<LinkContact>) {
        val array = JSONArray()
        contacts.forEach { array.put(JSONObject().put("name", it.name).put("username", it.username).put("aliases", JSONArray(it.aliases))) }
        preferences.edit().putString("contacts", array.toString()).apply()
    }

    fun cachedContacts(): List<LinkContact> = runCatching {
        parseContacts(JSONArray(preferences.getString("contacts", "[]")))
    }.getOrDefault(emptyList())
}

private fun parseContacts(array: JSONArray): List<LinkContact> = (0 until array.length()).map { index ->
    val item = array.getJSONObject(index)
    val aliases = item.optJSONArray("aliases") ?: JSONArray()
    LinkContact(item.getString("name"), item.getString("username"), (0 until aliases.length()).map { aliases.getString(it) })
}

class LinkRepository(private val preferences: LinkPreferences) {
    suspend fun checkGateway() = withContext(Dispatchers.IO) {
        val connection = URL(preferences.gatewayUrl + "/").openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            if (connection.responseCode != 200) throw IOException("El servidor respondió, pero no es el backend esperado de Link.")
            val data = JSONObject(connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
            if (data.optString("status") != "gateway alive") throw IOException("La dirección responde, pero no corresponde al backend de Link.")
        } catch (error: java.net.SocketTimeoutException) {
            throw IOException("No respondió ${preferences.gatewayUrl}. ${connectionHint()}", error)
        } catch (error: java.net.ConnectException) {
            throw IOException("No pude conectar a ${preferences.gatewayUrl}. ${connectionHint()}", error)
        } catch (error: java.net.UnknownHostException) {
            throw IOException("No encontré el servidor. Revisa la dirección del backend.", error)
        } catch (error: org.json.JSONException) {
            throw IOException("La dirección respondió, pero no devolvió una respuesta válida de Link.", error)
        } finally {
            connection.disconnect()
        }
    }

    private fun connectionHint(): String = if (URL(preferences.gatewayUrl).host in setOf("127.0.0.1", "localhost"))
        "Conserva el cable conectado y restablece el enlace USB desde la computadora."
    else "Comprueba la IP actual de la computadora y que la red permita conectar ambos dispositivos."

    suspend fun message(text: String): LinkResponse = request("/app/message", JSONObject()
        .put("install_id", preferences.installId).put("message", text.trim()))

    suspend fun contacts(): LinkResponse = request("/app/contacts?install_id=${preferences.installId}")

    suspend fun saveContact(name: String, username: String, aliases: List<String>): LinkResponse = request(
        "/app/contacts", JSONObject().put("install_id", preferences.installId)
            .put("name", name.trim()).put("username", username.trim()).put("aliases", JSONArray(aliases)),
    )

    private suspend fun request(path: String, body: JSONObject? = null): LinkResponse = withContext(Dispatchers.IO) {
        if (!preferences.configured) throw IOException("Configura la conexión con el backend desde el panel del cuidador.")
        val connection = URL(preferences.gatewayUrl + path).openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 10000
            connection.readTimeout = if (path.startsWith("/app/contacts")) 30000 else 95000
            connection.setRequestProperty("X-App-Secret", preferences.appSecret)
            if (body != null) {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            when (connection.responseCode) {
                200 -> {
                    val data = JSONObject(connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
                    LinkResponse(
                        type = data.optString("type", "info"), reply = data.optString("reply", "Sin respuesta del asistente."),
                        summary = data.optString("summary", ""),
                        actionUrl = if (data.isNull("action_url")) null else data.optString("action_url").takeIf { it.isNotBlank() },
                        contacts = data.optJSONArray("contacts")?.let(::parseContacts),
                    )
                }
                401, 403 -> throw IOException("La clave de conexión no coincide. Revísala en Configuración.")
                422 -> throw IOException("Revisa los datos que escribiste e intenta de nuevo.")
                else -> throw IOException("El backend no pudo responder. Revisa su terminal e intenta de nuevo.")
            }
        } catch (error: java.net.SocketTimeoutException) {
            throw IOException("La solicitud tardó demasiado. Revisa la conexión y el backend. Si estabas guardando un contacto, actualiza la lista antes de repetir.", error)
        } catch (error: java.net.ConnectException) {
            throw IOException("No pude conectar a ${preferences.gatewayUrl}. ${connectionHint()}", error)
        } finally {
            connection.disconnect()
        }
    }
}
