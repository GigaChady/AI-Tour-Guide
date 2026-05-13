package ai.tour.guide.domain.auth

import ai.tour.guide.data.appData.AppDataRepository
import ai.tour.guide.network.rest.ApiClient
import ai.tour.guide.network.schema.response.TokenResponseDto
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integration tests for AuthService that use a real AppDataRepository (backed by DataStore).
 * These verify the full flow from HTTP response → DTO deserialization → DataStore persistence,
 * unlike unit tests which mock AppDataRepository.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AuthServiceIntegrationTest {

    private lateinit var appDataRepository: AppDataRepository

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Application>()
        appDataRepository = AppDataRepository(context)
        appDataRepository.clearSessionData()
    }

    @Test
    fun `login success persists access token and refresh token to DataStore`() = runTest {
        val tokenResponse = TokenResponseDto(accessToken = "access-token", refreshToken = "refresh-token")
        val service = AuthService(
            apiClient = createApiClient(Json.encodeToString(tokenResponse)),
            appDataRepository = appDataRepository
        )

        val result = service.login("test@example.com", "password")

        assertTrue(result.isSuccessful)
        assertEquals("access-token", appDataRepository.bearerTokenFlow.value)
        assertEquals("refresh-token", appDataRepository.getRefreshToken())
    }

    @Test
    fun `login failure does not modify DataStore credentials`() = runTest {
        val service = AuthService(
            apiClient = createApiClient(
                jsonResponse = """{"detail":"Invalid credentials"}""",
                status = HttpStatusCode.Unauthorized
            ),
            appDataRepository = appDataRepository
        )

        val result = service.login("test@example.com", "wrong-password")

        assertFalse(result.isSuccessful)
        assertNull(appDataRepository.bearerTokenFlow.value)
    }

    @Test
    fun `register success persists access token and refresh token to DataStore`() = runTest {
        val tokenResponse = TokenResponseDto(accessToken = "new-access", refreshToken = "new-refresh")
        val service = AuthService(
            apiClient = createApiClient(Json.encodeToString(tokenResponse)),
            appDataRepository = appDataRepository
        )

        val result = service.register("Alice", "alice@example.com", "password")

        assertTrue(result.isSuccessful)
        assertEquals("new-access", appDataRepository.bearerTokenFlow.value)
        assertEquals("new-refresh", appDataRepository.getRefreshToken())
    }

    private fun createApiClient(
        jsonResponse: String,
        status: HttpStatusCode = HttpStatusCode.OK
    ): ApiClient {
        val mockEngine = MockEngine { _ ->
            respond(
                content = jsonResponse,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return ApiClient(appDataRepository, httpClient)
    }
}
