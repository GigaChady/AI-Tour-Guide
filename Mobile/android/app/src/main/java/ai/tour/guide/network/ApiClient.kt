package ai.tour.guide.network

import ai.tour.guide.config.AppConfig
import ai.tour.guide.network.schema.response.BaseAPIResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single
class ApiClient {
    suspend inline fun <reified T : BaseAPIResponse> get(route: ApiClientRoute): ApiClientRequestResult<T> {
        return try {
            val response = httpClient.get {
                url {
                    protocol = AppConfig.HTTPS_CLIENT_PROTOCOL
                    host = AppConfig.HTTPS_CLIENT_HOST
                    path(route.path)
                }
            }
            ApiClientRequestResult(response, response.body<T>())
        } catch (e: Exception) {
            ApiClientRequestResult(e)
        }
    }

    suspend inline fun <reified D, reified T : BaseAPIResponse> post(
        route: ApiClientRoute,
        data: D
    ): ApiClientRequestResult<T> {
        return try {
            val response = httpClient.post {
                url {
                    protocol = AppConfig.HTTPS_CLIENT_PROTOCOL
                    host = AppConfig.HTTPS_CLIENT_HOST
                    path(route.path)
                }
                contentType(ContentType.Application.Json)
                setBody(data)
            }
            ApiClientRequestResult(response, response.body<T>())
        } catch (e: Exception) {
            ApiClientRequestResult(e)
        }
    }

    companion object {
        val httpClient = HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }
    }
}

enum class ApiClientRoute(val path: String) {
    AUTH_LOGIN("/auth/login")
}

class ApiClientRequestResult<T> {
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
            val response = httpResponse?.status ?: return false
            response.value.let {
                return it in 100..399
            }
        }
}
