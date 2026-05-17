package com.hangout.app.network

import com.hangout.app.data.*
import com.hangout.app.ui.eventdetail.CancelRsvpRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────────

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<MessageResponse>

    // ── User ──────────────────────────────────────────────────────────────

    @GET("user/profile")
    suspend fun getProfile(): Response<UserProfile>

    @PUT("user/profile")
    suspend fun updateAdditionalInfo(
        @Body body: @JvmSuppressWildcards Map<String, String>
    ): Response<MessageResponse>

    @GET("user/notifications/preferences")
    suspend fun getNotificationPreferences(): Response<Map<String, Any>>

    @PUT("user/notifications/preferences")
    suspend fun updateNotificationPreferences(
        @Body body: @JvmSuppressWildcards Map<String, Boolean>
    ): Response<MessageResponse>

    @DELETE("user/account")
    suspend fun deleteAccount(): Response<MessageResponse>

    @GET("user/stats")
    suspend fun getStats(): Response<UserStats>

    @GET("user/photo")
    suspend fun getPhoto(): Response<PhotoResponse>

    @PUT("user/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<MessageResponse>

    @PUT("user/password")
    suspend fun updatePassword(@Body request: UpdatePasswordRequest): Response<MessageResponse>

    @Multipart
    @POST("user/photo")
    suspend fun uploadPhoto(@Part photo: MultipartBody.Part): Response<MessageResponse>

    @DELETE("user/photo")
    suspend fun deletePhoto(): Response<MessageResponse>

    // ── Events — Discovery ────────────────────────────────────────────────

    @GET("events/discover")
    suspend fun getDiscoverEvents(
        @Query("search") search: String = "",
        @Query("filter") filter: String = ""
    ): Response<List<EventItem>>

    @GET("events/{id}")
    suspend fun getEventById(@Path("id") id: Long): Response<EventItem>

    @GET("events/today")
    suspend fun getTodayEvents(): Response<List<EventItem>>

    @GET("events/hosting")
    suspend fun getHostingEvents(): Response<List<EventItem>>

    // ── Events — Creation & Management ────────────────────────────────────

    @POST("events")
    suspend fun createEvent(@Body request: CreateEventRequest): Response<CreateEventResponse>

    @PUT("events/{id}")
    suspend fun updateEvent(
        @Path("id") id: Long,
        @Body request: CreateEventRequest
    ): Response<CreateEventResponse>

    @Multipart
    @POST("events/{eventId}/cover-image")
    suspend fun uploadEventCoverImage(
        @Path("eventId") eventId: Long,
        @Part file: MultipartBody.Part
    ): Response<Map<String, Any>>

    @DELETE("events/{id}")
    suspend fun deleteEvent(@Path("id") id: Long): Response<MessageResponse>

    // ── RSVP ──────────────────────────────────────────────────────────────

    @POST("events/{id}/rsvp")
    suspend fun rsvpEvent(@Path("id") id: Long): Response<RsvpResponse>

    @HTTP(method = "DELETE", path = "events/{eventId}/rsvp", hasBody = true)
    suspend fun cancelRsvp(
        @Path("eventId") eventId: Long,
        @Body body: CancelRsvpRequest
    ): Response<MessageResponse>

    @GET("events/{id}/rsvp/check")
    suspend fun checkRsvpStatus(@Path("id") id: Long): Response<RsvpStatusResponse>

    @GET("events/attending")
    suspend fun getAttendingEvents(): Response<List<EventItem>>

    // ── Host Dashboard ────────────────────────────────────────────────────

    @GET("events/{id}/attendees")
    suspend fun getEventAttendees(@Path("id") id: Long): Response<List<AttendeeItem>>

    @POST("events/{eventId}/rsvp/{rsvpId}/approve")
    suspend fun approvePayment(
        @Path("eventId") eventId: Long,
        @Path("rsvpId")  rsvpId: Long,
        @Body body: ApprovePaymentRequest
    ): Response<MessageResponse>

    @POST("events/{eventId}/rsvp/{rsvpId}/reject")
    suspend fun rejectPayment(
        @Path("eventId") eventId: Long,
        @Path("rsvpId")  rsvpId: Long,
        @Body body: RejectPaymentRequest
    ): Response<MessageResponse>

    @POST("events/{eventId}/rsvp/{rsvpId}/assign-seat")
    suspend fun assignSeat(
        @Path("eventId") eventId: Long,
        @Path("rsvpId")  rsvpId: Long,
        @Body body: AssignSeatRequest
    ): Response<MessageResponse>

    @POST("events/{eventId}/rsvp/{rsvpId}/confirm-attendee")
    suspend fun confirmAttendee(
        @Path("eventId") eventId: Long,
        @Path("rsvpId")  rsvpId: Long
    ): Response<MessageResponse>

    @POST("events/{eventId}/rsvp/{rsvpId}/reject-attendee")
    suspend fun rejectAttendee(
        @Path("eventId") eventId: Long,
        @Path("rsvpId")  rsvpId: Long,
        @Body body: RejectPaymentRequest
    ): Response<MessageResponse>

    @POST("events/{id}/cancel")
    suspend fun cancelEvent(
        @Path("id") id: Long,
        @Body body: @JvmSuppressWildcards Map<String, String>
    ): Response<MessageResponse>

    @Multipart
    @POST("events/{id}/rsvp/payment-proof")
    suspend fun uploadPaymentProof(
        @Path("id") eventId: Long,
        @Part proof: MultipartBody.Part
    ): Response<MessageResponse>

    @POST("events/{id}/rsvp/refund")
    suspend fun requestRefund(
        @Path("id") eventId: Long,
        @Body body: @JvmSuppressWildcards Map<String, String>
    ): Response<MessageResponse>

    @POST("events/{id}/rsvp/acknowledge-refund")
    suspend fun acknowledgeRefund(
        @Path("id") eventId: Long,
        @Body body: @JvmSuppressWildcards Map<String, String?>
    ): Response<MessageResponse>

    @POST("events/{eventId}/rsvps/{rsvpId}/attendance")
    suspend fun markAttendance(
        @Path("eventId") eventId: Long,
        @Path("rsvpId")  rsvpId: Long,
        @Body body: @JvmSuppressWildcards Map<String, String>
    ): Response<MessageResponse>

    // ── Refund — Host ─────────────────────────────────────────────────────
    @Multipart
    @POST("events/{eventId}/rsvp/{rsvpId}/approve-refund")
    suspend fun approveRefund(
        @Path("eventId") eventId: Long,
        @Path("rsvpId")  rsvpId:  Long,
        @Part            proofImage: MultipartBody.Part
    ): Response<MessageResponse>

    @POST("events/{eventId}/rsvp/{rsvpId}/reject-refund")
    suspend fun rejectRefund(
        @Path("eventId") eventId: Long,
        @Path("rsvpId")  rsvpId: Long,
        @Body body: @JvmSuppressWildcards Map<String, String>
    ): Response<MessageResponse>

    // ── Ticket verification ───────────────────────────────────────────────

    @GET("events/{eventId}/rsvp/verify/{ticketToken}")
    suspend fun verifyTicket(
        @Path("eventId")     eventId:     Long,
        @Path("ticketToken") ticketToken: String
    ): Response<Map<String, Any>>

    // ── Favorites ─────────────────────────────────────────────────────────

    @POST("events/{id}/favorite")
    suspend fun addFavorite(@Path("id") eventId: Long): Response<MessageResponse>

    @DELETE("events/{id}/favorite")
    suspend fun removeFavorite(@Path("id") eventId: Long): Response<MessageResponse>

    @GET("events/favorites/list")
    suspend fun getFavoriteEvents(): Response<List<EventItem>>

    @GET("events/{id}/favorite/check")
    suspend fun checkFavorite(@Path("id") eventId: Long): Response<FavoriteStatusResponse>

    // ── Notifications ─────────────────────────────────────────────────────

    @GET("notifications")
    suspend fun getNotifications(): Response<List<Map<String, Any>>>

    @GET("notifications/unread/count")
    suspend fun getUnreadNotificationCount(): Response<Map<String, Any>>

    @PATCH("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: Long): Response<Map<String, Any>>

    @PATCH("notifications/read-all")
    suspend fun markAllNotificationsRead(): Response<Map<String, Any>>

    @DELETE("notifications/{id}")
    suspend fun deleteNotification(@Path("id") id: Long): Response<Map<String, Any>>

    @DELETE("notifications")
    suspend fun deleteAllNotifications(): Response<Map<String, Any>>

    // ── Messaging ─────────────────────────────────────────────────────────

    @GET("messages/conversations")
    suspend fun getConversations(): Response<List<Map<String, Any>>>

    @GET("messages/conversation/{userId}")
    suspend fun getConversation(@Path("userId") userId: Long): Response<Map<String, Any>>

    @POST("messages/send")
    suspend fun sendMessage(
        @Body body: @JvmSuppressWildcards Map<String, Any>
    ): Response<Map<String, Any>>

    @GET("messages/unread/count")
    suspend fun getUnreadMessageCount(): Response<Map<String, Any>>


    @GET("users/{id}/profile")
    suspend fun getPublicUserProfile(@Path("id") id: Long): Response<Map<String, Any>>
}