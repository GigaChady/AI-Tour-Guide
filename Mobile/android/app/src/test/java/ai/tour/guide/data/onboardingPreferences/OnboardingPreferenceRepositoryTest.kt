package ai.tour.guide.data.onboardingPreferences

import ai.tour.guide.data.appData.AppDataRepository
import ai.tour.guide.network.rest.ApiClient
import ai.tour.guide.network.schema.response.OnboardingPreferencesResponseDto
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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class OnboardingPreferenceRepositoryTest {

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
        val appDataRepository = mockk<AppDataRepository>(relaxed = true)
        every { appDataRepository.bearerTokenFlow } returns MutableStateFlow(null)

        return ApiClient(appDataRepository, httpClient)
    }

    @Test
    fun `fetchPreferencesIfEmpty fetches preferences when flow is empty`() = runTest {
        val mockPreferences = listOf(OnboardingPreferencesDto(key = "pref1", title = "Title 1"))
        val json = Json.encodeToString(
            OnboardingPreferencesResponseDto(
                items = mockPreferences,
                selectedAnswers = buildJsonObject {
                    put("pref1", "answer1")
                    putJsonArray("pref2") {}
                }
            )
        )
        val apiClient = createMockClient(json)
        val repository = OnboardingPreferenceRepository(apiClient)

        val response = repository.fetchPreferencesIfEmpty()

        assertEquals(mockPreferences, repository.preferences.value)
        assertNotNull(response)
        assertEquals("answer1", response?.getSelectedAnswer("pref1"))
        assertEquals(emptyList<String>(), response?.getSelectedAnswers("pref2"))
    }

    @Test
    fun `fetchPreferencesIfEmpty does not fetch when flow is not empty`() = runTest {
        val mockPreferences = listOf(OnboardingPreferencesDto(key = "pref1", title = "Title 1"))
        val json = Json.encodeToString(
            OnboardingPreferencesResponseDto(items = mockPreferences)
        )

        var callCount = 0
        val apiClient = createMockClient(json) {
            callCount++
        }
        val repository = OnboardingPreferenceRepository(apiClient)

        // First call populates it
        repository.fetchPreferencesIfEmpty()
        assertEquals(1, callCount)

        // Second call should skip fetching
        repository.fetchPreferencesIfEmpty()
        assertEquals(1, callCount)
    }

    @Test
    fun `fetchPreferences handles error responses`() = runTest {
        val json = "{\"detail\": \"Error\"}"
        val apiClient = createMockClient(json, HttpStatusCode.InternalServerError)
        val repository = OnboardingPreferenceRepository(apiClient)

        val response = repository.fetchPreferencesIfEmpty()

        assertEquals(emptyList<OnboardingPreferencesDto>(), repository.preferences.value)
        assertEquals(null, response)
    }
}
