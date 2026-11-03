package com.zarfolio.feature.authentication

import com.zarfolio.core.network.ApiClient
import com.zarfolio.core.network.NativeLoginRequest
import com.zarfolio.core.network.NativeLoginResponse
import com.zarfolio.core.network.ServerConnectionRepository
import com.zarfolio.core.network.ZarfolioApi
import com.zarfolio.core.security.TokenStore
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class LoginOutcome(
    val authenticated: Boolean,
    val userName: String? = null,
    val mfaRequired: Boolean = false,
    val mfaId: String? = null,
    val otpId: String? = null,
    val authenticatorAvailable: Boolean = false,
    val emailOtpAvailable: Boolean = false,
    val message: String? = null,
)

@Singleton
class LoginRepository @Inject constructor(
    private val servers: ServerConnectionRepository,
    private val apiClient: ApiClient,
    private val tokenStore: TokenStore,
) {
    private val responseJson = Json { ignoreUnknownKeys = true; explicitNulls = false }

    suspend fun login(
        loginId: String,
        password: String,
        mfaId: String? = null,
        otpId: String? = null,
        code: String? = null,
        authMethod: String? = null,
    ): LoginOutcome {
        val server = servers.active() ?: throw IOException("ابتدا نشانی سرور را تنظیم کنید")
        val api = apiClient.retrofitFor(server).create(ZarfolioApi::class.java)
        val response = api.login(
            request = NativeLoginRequest(
                email = loginId,
                password = password,
                mfaId = mfaId,
                otpId = otpId,
                otpCode = if (authMethod == "email") code else null,
                totpCode = if (authMethod == "totp") code else null,
                authMethod = authMethod,
            ),
        )
        // PocketBase challenges are intentionally returned as HTTP 401. Retrofit
        // places that JSON in errorBody(), so parse it before treating the request
        // as a failed password attempt.
        val body = response.body() ?: response.errorBody()?.string()?.let { raw ->
            runCatching { responseJson.decodeFromString<NativeLoginResponse>(raw) }.getOrNull()
        }
        if (response.isSuccessful && body?.success == true) {
            val token = body.token?.takeIf(String::isNotBlank) ?: throw IOException("توکن ورود از سرور دریافت نشد")
            tokenStore.write(server.id, token)
            val userName = body.user?.name?.trim()?.takeIf(String::isNotBlank)
                ?: body.user?.email?.trim()?.takeIf(String::isNotBlank)
                ?: loginId
            tokenStore.writeUserName(server.id, userName)
            return LoginOutcome(authenticated = true, userName = userName, message = "ورود با موفقیت انجام شد")
        }
        if (body?.mfaRequired == true) {
            return LoginOutcome(
                authenticated = false,
                mfaRequired = true,
                mfaId = body.mfaId,
                otpId = body.otpId,
                authenticatorAvailable = body.authenticatorAvailable,
                emailOtpAvailable = body.emailOtpAvailable,
                message = body.message ?: "کد تأیید دومرحله‌ای را وارد کنید",
            )
        }
        throw IOException(body?.message ?: if (response.code() == 429) "تعداد تلاش‌ها بیش از حد مجاز است" else "نام کاربری یا رمز عبور نادرست است")
    }
}
