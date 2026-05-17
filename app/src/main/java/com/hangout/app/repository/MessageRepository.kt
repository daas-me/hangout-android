package com.hangout.app.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hangout.app.data.*
import com.hangout.app.network.RetrofitClient

class MessageRepository(private val context: Context) {

    private val api = RetrofitClient.getApiService(context)
    private val gson = Gson()

    suspend fun getConversations(): Result<List<ConversationItem>> {
        return try {
            val r = api.getConversations()
            if (r.isSuccessful) {
                val body = r.body() ?: emptyList()
                val items = body.mapNotNull { parseConversation(it) }
                Result.Success(items)
            } else Result.Error("Failed to load conversations")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun getConversation(userId: Long, fallbackUser: OtherUser? = null): Result<ConversationDetail> {
        return try {
            val r = api.getConversation(userId)
            when {
                r.isSuccessful && r.body() != null -> {
                    val body = r.body()!!
                    val otherUser = parseUser(body["otherUser"] as? Map<*, *>)
                        ?: fallbackUser
                        ?: return Result.Error("Could not parse user data")
                    val messagesRaw = body["messages"] as? List<*> ?: emptyList<Any>()
                    val messages = messagesRaw.mapNotNull { parseMessage(it as? Map<*, *>) }
                    Result.Success(ConversationDetail(messages, otherUser))
                }
                r.code() == 404 -> {
                    // No conversation exists yet — return empty so the user can start chatting
                    val user = fallbackUser ?: return Result.Error("User not found")
                    Result.Success(ConversationDetail(emptyList(), user))
                }
                else -> {
                    Result.Error("Failed to load messages (${r.code()})")
                }
            }
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun sendMessage(recipientId: Long, content: String): Result<MessageItem> {
        return try {
            val r = api.sendMessage(mapOf("recipientId" to recipientId, "content" to content))
            if (r.isSuccessful && r.body() != null) {
                val msg = parseMessage(r.body()!!) ?: return Result.Error("Invalid response")
                Result.Success(msg)
            } else {
                val err = try {
                    org.json.JSONObject(r.errorBody()?.string() ?: "")
                        .optString("message", "Failed to send")
                } catch (_: Exception) { "Failed to send" }
                Result.Error(err)
            }
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun getUnreadCount(): Result<Int> {
        return try {
            val r = api.getUnreadMessageCount()
            if (r.isSuccessful) {
                val count = (r.body()?.get("unreadCount") as? Double)?.toInt() ?: 0
                Result.Success(count)
            } else Result.Error("Failed")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    private fun parseConversation(map: Map<*, *>?): ConversationItem? {
        val map2 = map ?: return null
        val otherUser = parseUser(map2["otherUser"] as? Map<*, *>) ?: return null
        val lastMessage = parseMessage(map2["lastMessage"] as? Map<*, *>) ?: return null
        val unread = ((map2["unreadCount"] as? Double)?.toLong()) ?: 0L
        return ConversationItem(otherUser, lastMessage, unread)
    }

    private fun parseUser(map: Map<*, *>?): OtherUser? {
        val m = map ?: return null
        return OtherUser(
            id = ((m["id"] as? Double)?.toLong()) ?: return null,
            firstname = m["firstname"] as? String,
            lastname = m["lastname"] as? String,
            email = m["email"] as? String,
            photo = m["photo"] as? String
        )
    }

    private fun parseMessage(map: Map<*, *>?): MessageItem? {
        val m = map ?: return null
        return MessageItem(
            id = ((m["id"] as? Double)?.toLong()) ?: return null,
            senderId = ((m["senderId"] as? Double)?.toLong()) ?: return null,
            recipientId = ((m["recipientId"] as? Double)?.toLong()) ?: return null,
            content = m["content"] as? String ?: "",
            isRead = m["isRead"] as? Boolean ?: false,
            sentAt = m["sentAt"] as? String
        )
    }
}