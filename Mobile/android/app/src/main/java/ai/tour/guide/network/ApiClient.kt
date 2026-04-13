package ai.tour.guide.network

import ai.tour.guide.config.AppConfig
import ai.tour.guide.data.appData.AppDataRepository
import ai.tour.guide.network.schema.response.IAPIResponseDto
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single
class ApiClient(val appDataRepository: AppDataRepository) {
    suspend inline fun <reified T> getList(route: ApiClientRoute): ApiObjectListResponse<T> {
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
            ApiObjectListResponse(response, response.body<List<T>>())
        } catch (e: Exception) {
            Log.e("ApiClient", "Error while making request", e)
            ApiObjectListResponse(e)
        }
    }

    val jsonParser = Json { ignoreUnknownKeys = true }

    suspend inline fun <reified T : IAPIResponseDto> get(route: ApiClientRoute): ApiBaseResponseResult<T> {
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

            val responseBody = response.bodyAsText()
            val isList = responseBody.trim().startsWith("[")

            ApiBaseResponseResult(
                response,
                if (isList) null else jsonParser.decodeFromString<T>(
                    responseBody
                )
            )
        } catch (e: Exception) {
            Log.e("ApiClient", "Error while making request", e)
            ApiBaseResponseResult(e)
        }
    }

    suspend inline fun <reified D, reified T : IAPIResponseDto> post(
        route: ApiClientRoute,
        data: D
    ): ApiBaseResponseResult<T> {
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

            val responseBody = response.bodyAsText()
            val isList = responseBody.trim().startsWith("[")

            ApiBaseResponseResult(
                response,
                if (isList) null else jsonParser.decodeFromString<T>(
                    responseBody
                )
            )
        } catch (e: Exception) {
            Log.e("ApiClient", "Error while making request", e)
            ApiBaseResponseResult(e)
        }
    }

    companion object {
        val httpClient = HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }
}

enum class ApiClientRoute(val path: String) {
    AUTH_LOGIN("/auth/login"),
    AUTH_REGISTER("/auth/register"),
    AUTH_GOOGLE("/auth/google"),
    ONBOARDING_QUESTIONS("/user/onboarding/questions"),
    ONBOARDING_ANSWERS("/user/onboarding/answers")
}

class ApiObjectListResponse<T> {
    val body: List<T>?
    val exception: Exception?
    private val httpResponse: HttpResponse?

    constructor(exception: Exception) {
        this.exception = exception
        this.httpResponse = null
        this.body = null
    }

    constructor(httpResponse: HttpResponse, body: List<T>?) {
        this.httpResponse = httpResponse
        this.body = body
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

    val errorMessage: String
        get() {
            return exception?.message ?: "Unknown error"
        }
}

class ApiBaseResponseResult<T : IAPIResponseDto> {
    val body: T?
    val exception: Exception?
    private val httpResponse: HttpResponse?

    constructor(exception: Exception) {
        this.exception = exception
        this.httpResponse = null
        this.body = null
    }

    constructor(httpResponse: HttpResponse, body: T?) {
        this.httpResponse = httpResponse
        this.body = body
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

    val errorMessage: String
        get() {
            if (body?.detail != null) return body.detail!!
            return exception?.message ?: "Unknown error"
        }
}
