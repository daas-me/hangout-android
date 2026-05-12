package com.hangout.app.network

import com.hangout.app.data.*
import okhttp3.MultipartBody
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

    @DELETE("events/{id}/rsvp")
    suspend fun cancelRsvp(@Path("id") id: Long): Response<MessageResponse>

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
        @Body body: Map<String, String>
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
        @Body body: Map<String, String>
    ): Response<MessageResponse>

    @POST("events/{id}/rsvp/acknowledge-refund")
    suspend fun acknowledgeRefund(
        @Path("id") eventId: Long
    ): Response<MessageResponse>

    // ── Notifications ─────────────────────────────────────────────────────

    @GET("notifications")
    suspend fun getNotifications(): Response<List<Map<String, Any>>>

    @GET("notifications/unread-count")
    suspend fun getUnreadNotificationCount(): Response<Map<String, Any>>

    @PUT("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: Long): Response<Map<String, Any>>

    @PUT("notifications/read-all")
    suspend fun markAllNotificationsRead(): Response<Map<String, Any>>
}
