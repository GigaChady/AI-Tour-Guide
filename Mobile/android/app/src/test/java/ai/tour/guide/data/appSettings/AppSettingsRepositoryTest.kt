package ai.tour.guide.data.appSettings

import ai.tour.guide.data.appData.AppDataRepository
import ai.tour.guide.network.rest.ApiClient
import ai.tour.guide.network.schema.response.AppSettingsResponseDto
import android.app.Application
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AppSettingsRepositoryTest {

    private fun createMockClient(
        jsonResponse: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        onCall: () -> Unit = {}
    ): ApiClient {
        val mockEngine = MockEngine { _ ->
            onCall()
            respond(
                content = jsonResponse,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val appDataRepository = mockk<AppDataRepository>(relaxed = true)
        every { appDataRepository.bearerTokenFlow } returns MutableStateFlow(null)
        return ApiClient(appDataRepository, httpClient)
    }

    @Test
    fun `settings flow is null before first fetch`() {
        val repository = AppSettingsRepository(createMockClient("{}"))
        assertNull(repository.settings.value)
    }

    @Test
    fun `fetchSettingsIfEmpty fetches and populates settings flow when cache is empty`() = runTest {
        val response = AppSettingsResponseDto(language = "pl", pitch = 60)
        val repository = AppSettingsRepository(createMockClient(Json.encodeToString(response)))

        val result = repository.fetchSettingsIfEmpty()

        assertNotNull(result)
        assertEquals("pl", result?.language)
        assertEquals(60, result?.pitch)
        assertEquals("pl", repository.settings.value?.language)
    }

    @Test
    fun `fetchSettingsIfEmpty skips API call when cache is already populated`() = runTest {
        val response = AppSettingsResponseDto(language = "en")
        var callCount = 0
        val repository = AppSettingsRepository(createMockClient(Json.encodeToString(response)) { callCount++ })

        repository.fetchSettingsIfEmpty()
        assertEquals(1, callCount)

        repository.fetchSettingsIfEmpty()
        assertEquals(1, callCount)
    }

    @Test
    fun `fetchSettingsIfEmpty returns null on API error`() = runTest {
        val repository = AppSettingsRepository(
            createMockClient("""{"detail":"error"}""", HttpStatusCode.InternalServerError)
        )

        val result = repository.fetchSettingsIfEmpty()

        assertNull(result)
        assertNull(repository.settings.value)
    }
}
