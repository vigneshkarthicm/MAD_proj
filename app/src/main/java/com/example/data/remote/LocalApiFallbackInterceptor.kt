package com.example.data.remote

import com.example.data.repository.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class LocalApiFallbackInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        try {
            return chain.proceed(request)
        } catch (e: IOException) {
            if (sessionManager.isSandboxBackend.value) {
                throw e
            }

            val originalUrl = request.url
            val host = originalUrl.host

            // If a request to a local LAN IP (10.x.x.x, 192.168.x.x, 10.0.2.2) fails due to Wi-Fi isolation,
            // attempt fallback to 127.0.0.1:8000 via ADB reverse tunnel
            if ((host != "127.0.0.1") && (host != "localhost") && (host.startsWith("10.") || host.startsWith("192.168."))) {
                val fallbackUrl = originalUrl.newBuilder()
                    .host("127.0.0.1")
                    .port(8000)
                    .build()

                val fallbackRequest = request.newBuilder()
                    .url(fallbackUrl)
                    .build()

                return try {
                    val response = chain.proceed(fallbackRequest)
                    if (response.isSuccessful) {
                        sessionManager.setApiBaseUrl("http://127.0.0.1:8000/api/v1/")
                    }
                    response
                } catch (_: Exception) {
                    throw e
                }
            } else {
                throw e
            }
        }
    }
}
