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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class OnboardingPreferenceRepositoryTest {

    private val originalLocale = Locale.getDefault()

    @Before
    fun setUp() {
        Locale.setDefault(Locale.ENGLISH)
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    private fun createMockClient(
        jsonResponse: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        onCall: (url: String) -> Unit = {}
    ): ApiClient {
        val mockEngine = MockEngine { request ->
            onCall(request.url.toString())
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
        val repository = OnboardingPreferenceRepository(createMockClient(json))

        val response = repository.fetchPreferencesIfEmpty()

        assertEquals(mockPreferences, repository.preferences.value)
        assertNotNull(response)
        assertEquals("answer1", response?.getSelectedAnswer("pref1"))
        assertEquals(emptyList<String>(), response?.getSelectedAnswers("pref2"))
    }

    @Test
    fun `fetchPreferencesIfEmpty does not fetch when flow is not empty and lang unchanged`() = runTest {
        val json = Json.encodeToString(
            OnboardingPreferencesResponseDto(items = listOf(OnboardingPreferencesDto(key = "pref1")))
        )
        var callCount = 0
        val repository = OnboardingPreferenceRepository(createMockClient(json) { callCount++ })

        repository.fetchPreferencesIfEmpty()
        assertEquals(1, callCount)

        repository.fetchPreferencesIfEmpty()
        assertEquals(1, callCount)
    }

    @Test
    fun `fetchPreferencesIfEmpty refetches when device language changes`() = runTest {
        val json = Json.encodeToString(
            OnboardingPreferencesResponseDto(items = listOf(OnboardingPreferencesDto(key = "pref1")))
        )
        var callCount = 0
        val lastUrl = mutableListOf<String>()
        val repository = OnboardingPreferenceRepository(createMockClient(json) { url ->
            callCount++
            lastUrl.add(url)
        })

        Locale.setDefault(Locale.forLanguageTag("en"))
        repository.fetchPreferencesIfEmpty()
        assertEquals(1, callCount)
        assert(lastUrl.last().contains("lang=en"))

        Locale.setDefault(Locale.forLanguageTag("pl"))
        repository.fetchPreferencesIfEmpty()
        assertEquals(2, callCount)
        assert(lastUrl.last().contains("lang=pl"))
    }

    @Test
    fun `fetchPreferencesIfEmpty sends device lang query param`() = runTest {
        Locale.setDefault(Locale.forLanguageTag("pl"))
        val json = Json.encodeToString(OnboardingPreferencesResponseDto())
        var capturedUrl = ""
        val repository = OnboardingPreferenceRepository(createMockClient(json) { url -> capturedUrl = url })

        repository.fetchPreferencesIfEmpty()

        assert(capturedUrl.contains("lang=pl")) { "Expected lang=pl in URL: $capturedUrl" }
    }

    @Test
    fun `fetchPreferences handles error responses`() = runTest {
        val json = "{\"detail\": \"Error\"}"
        val repository = OnboardingPreferenceRepository(
            createMockClient(json, HttpStatusCode.InternalServerError)
        )

        val response = repository.fetchPreferencesIfEmpty()

        assertEquals(emptyList<OnboardingPreferencesDto>(), repository.preferences.value)
        assertEquals(null, response)
    }
}
