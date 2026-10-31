package com.example.link.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppMessageRequest(
    @SerialName("install_id") val installId: String,
    val message: String,
)

@Serializable
enum class ReplyType {
    @SerialName("confirmation") CONFIRMATION,
    @SerialName("result") RESULT,
    @SerialName("denied") DENIED,
    @SerialName("info") INFO,
    @SerialName("error") ERROR,
}

@Serializable
data class AssistantReply(
    val type: ReplyType = ReplyType.ERROR,
    val reply: String = "",
    val summary: String? = null,
    @SerialName("action_url") val actionUrl: String? = null,
)