package ai.tour.guide.domain.auth

import ai.tour.guide.data.appData.AppDataRepository
import ai.tour.guide.network.ApiClient
import ai.tour.guide.network.schema.response.TokenResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthServiceTest {

    @Test
    fun `login success persists returned credentials`() = runTest {
        val appDataRepository = mockk<AppDataRepository>(relaxed = true)
        every { appDataRepository.bearerTokenFlow } returns MutableStateFlow(null)
        val tokenResponse = TokenResponseDto(accessToken = "access", refreshToken = "refresh")
        val service = AuthService(
            apiClient = createApiClient(appDataRepository, Json.encodeToString(tokenResponse)),
            appDataRepository = appDataRepository
        )

        val result = service.login("test@test.com", "password")

        assertTrue(result.isSuccessful)
        coVerify(exactly = 1) { appDataRepository.updateCredentialsWithAPIResponse(tokenResponse) }
    }

    @Test
    fun `register success persists returned credentials`() = runTest {
        val appDataRepository = mockk<AppDataRepository>(relaxed = true)
        every { appDataRepository.bearerTokenFlow } returns MutableStateFlow(null)
        val tokenResponse = TokenResponseDto(accessToken = "access", refreshToken = "refresh")
        val service = AuthService(
            apiClient = createApiClient(appDataRepository, Json.encodeToString(tokenResponse)),
            appDataRepository = appDataRepository
        )

        val result = service.register("Tester", "test@test.com", "password")

        assertTrue(result.isSuccessful)
        coVerify(exactly = 1) { appDataRepository.updateCredentialsWithAPIResponse(tokenResponse) }
    }

    @Test
    fun `login failure does not persist credentials`() = runTest {
        val appDataRepository = mockk<AppDataRepository>(relaxed = true)
        every { appDataRepository.bearerTokenFlow } returns MutableStateFlow(null)
        val service = AuthService(
            apiClient = createApiClient(
                appDataRepository,
                jsonResponse = """{"detail":"Invalid credentials"}""",
                status = HttpStatusCode.Unauthorized
            ),
            appDataRepository = appDataRepository
        )

        val result = service.login("test@test.com", "bad-password")

        assertTrue(!result.isSuccessful)
        coVerify(exactly = 0) { appDataRepository.updateCredentialsWithAPIResponse(any()) }
    }

    private fun createApiClient(
        appDataRepository: AppDataRepository,
        jsonResponse: String,
        status: HttpStatusCode = HttpStatusCode.OK
    ): ApiClient {
        val mockEngine = MockEngine { _ ->
            respond(
                content = jsonResponse,
                status = status,
                headers = headersOf(
                    HttpHeaders.ContentType,
                    ContentType.Application.Json.toString()
                )
            )
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        return ApiClient(appDataRepository, httpClient)
    }
}
