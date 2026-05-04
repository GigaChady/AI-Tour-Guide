package ai.tour.guide.network.rest

import ai.tour.guide.config.AppConfig
import ai.tour.guide.data.appData.AppDataRepository
import ai.tour.guide.network.schema.request.AuthRefreshRequestDto
import ai.tour.guide.network.schema.response.IAPIResponseDto
import ai.tour.guide.network.schema.response.TokenResponseDto
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single
class ApiClient(
    val appDataRepository: AppDataRepository,
    @PublishedApi internal val httpClient: HttpClient = defaultHttpClient
) {
    suspend fun fetchBearerToken() {
        val refreshToken = appDataRepository.getRefreshToken()
        val payload = AuthRefreshRequestDto(refreshToken)
        var authResponse: ApiResponse<TokenResponseDto>?

        try {
            val response = httpClient.post {
                url {
                    protocol = AppConfig.HTTPS_CLIENT_PROTOCOL
                    host = AppConfig.HTTPS_CLIENT_HOST
                    path(ApiClientRoute.AUTH_REFRESH.path)
                }
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            authResponse = if (response.status == HttpStatusCode.NoContent) {
                ApiResponse(response, null)
            } else {
                ApiResponse(
                    response,
                    response.body()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error while making request", e)
            authResponse = ApiResponse(e)
        }

        appDataRepository.updateBearerToken(authResponse.body)
    }

    suspend inline fun fetchBearerTokenIfNeeded() {
        if (appDataRepository.shouldRefreshBearerToken()) {
            fetchBearerToken()
        }
    }

    suspend inline fun <reified T : IAPIResponseDto> get(route: ApiClientRoute): ApiResponse<T> {
        fetchBearerTokenIfNeeded()
        return try {
            val response = httpClient.get {
                url {
                    protocol = AppConfig.HTTPS_CLIENT_PROTOCOL
                    host = AppConfig.HTTPS_CLIENT_HOST
                    path(route.path)
                }
                appDataRepository.bearerTokenFlow.value?.let { token ->
                    header("Authorization", "Bearer $token")
                }
            }
            ApiResponse(
                response,
                response.body()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error while making request", e)
            ApiResponse(e)
        }
    }

    suspend inline fun <reified D, reified T : IAPIResponseDto> post(
        route: ApiClientRoute,
        data: D
    ): ApiResponse<T> {
        fetchBearerTokenIfNeeded()
        return try {
            val response = httpClient.post {
                url {
                    protocol = AppConfig.HTTPS_CLIENT_PROTOCOL
                    host = AppConfig.HTTPS_CLIENT_HOST
                    path(route.path)
                }
                contentType(ContentType.Application.Json)
                appDataRepository.bearerTokenFlow.value?.let { token ->
                    header("Authorization", "Bearer $token")
                }
                setBody(data)
            }
            if (response.status == HttpStatusCode.NoContent) {
                return ApiResponse(response, null)
            }
            return ApiResponse(
                response,
                response.body()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error while making request", e)
            ApiResponse(e)
        }
    }

    companion object {
        const val TAG = "ApiClient"
    }
}

@PublishedApi
internal val defaultHttpClient = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

enum class ApiClientRoute(val path: String) {
    AUTH_LOGIN("/auth/login"),
    AUTH_REGISTER("/auth/register"),
    AUTH_GOOGLE("/auth/google"),
    AUTH_REFRESH("/auth/refresh"),
    USER_ONBOARDING_QUESTIONS("/user/onboarding/questions"),
    USER_ONBOARDING_ANSWERS("/user/onboarding/answers"),
    USER_ME_PARAMS("/user/me-params"),
}

class ApiResponse<T : IAPIResponseDto> : ApiBaseResponseResult {
    val body: T?

    constructor(exception: Exception) : super(exception) {
        this.body = null
    }

    constructor(httpResponse: HttpResponse, body: T?) : super(httpResponse) {
        this.body = body
    }

    override val errorMessage: String
        get() {
            if (body?.detail != null) return body.detail!!
            return super.errorMessage
        }
}

open class ApiBaseResponseResult {
    val exception: Exception?
    private val httpResponse: HttpResponse?

    constructor(exception: Exception) {
        this.exception = exception
        this.httpResponse = null
    }

    constructor(httpResponse: HttpResponse) {
        this.httpResponse = httpResponse
        this.exception = null
    }

    val isSuccessful: Boolean
        get() {
            if (exception != null) return false
            val response = httpResponse?.status ?: return false
            response.value.let {
                return it in 100..399
            }
        }

    open val errorMessage: String
        get() {
            return exception?.message ?: "Unknown error"
        }
}