package ai.tour.guide.domain.preferences

import ai.tour.guide.data.appData.AppDataRepository
import ai.tour.guide.data.onboardingPreferences.OnboardingPreferenceQuestionType
import ai.tour.guide.data.onboardingPreferences.OnboardingPreferenceRepository
import ai.tour.guide.data.onboardingPreferences.OnboardingPreferencesDto
import ai.tour.guide.network.rest.ApiClient
import ai.tour.guide.network.schema.response.EmptyAPIResponse
import ai.tour.guide.network.schema.response.OnboardingPreferencesResponseDto
import ai.tour.guide.ui.sharedFragments.preferences.UserPreferenceFragmentState
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingPreferencesServiceTest {

    @Test
    fun `preferences exposes repository flow`() {
        val repository = mockk<OnboardingPreferenceRepository>()
        val preferencesFlow =
            MutableStateFlow(emptyList<OnboardingPreferencesDto>())
        every { repository.preferences } returns preferencesFlow
        val service = OnboardingPreferencesService(
            onboardingPreferenceRepository = repository,
            apiClient = createApiClient()
        )

        assertSame(preferencesFlow, service.preferences)
    }

    @Test
    fun `fetchPreferencesIfEmpty maps selected answers into view state`() = runTest {
        val repository = mockk<OnboardingPreferenceRepository>(relaxed = true)
        every { repository.preferences } returns MutableStateFlow(emptyList())
        coEvery { repository.fetchPreferencesIfEmpty() } returns OnboardingPreferencesResponseDto(
            items = listOf(
                OnboardingPreferencesDto(
                    key = "gender",
                    type = OnboardingPreferenceQuestionType.SINGLE_CHOICE
                ),
                OnboardingPreferencesDto(
                    key = "interests",
                    type = OnboardingPreferenceQuestionType.MULTIPLE_CHOICE
                )
            ),
            selectedAnswers = mapOf(
                "gender" to JsonPrimitive("female"),
                "interests" to JsonArray(listOf(JsonPrimitive("food"), JsonPrimitive("history")))
            )
        )
        val service = OnboardingPreferencesService(
            onboardingPreferenceRepository = repository,
            apiClient = createApiClient()
        )

        val result = service.fetchPreferencesIfEmpty()

        coVerify(exactly = 1) { repository.fetchPreferencesIfEmpty() }
        assertEquals(
            UserPreferenceFragmentState(
                selectedSingleOptions = mapOf("gender" to "female"),
                selectedMultipleOptions = mapOf("interests" to setOf("food", "history"))
            ),
            result
        )
    }

    @Test
    fun `fetchPreferencesIfEmpty returns null when repository has no new response`() = runTest {
        val repository = mockk<OnboardingPreferenceRepository>(relaxed = true)
        every { repository.preferences } returns MutableStateFlow(emptyList())
        coEvery { repository.fetchPreferencesIfEmpty() } returns null
        val service = OnboardingPreferencesService(
            onboardingPreferenceRepository = repository,
            apiClient = createApiClient()
        )

        val result = service.fetchPreferencesIfEmpty()

        assertEquals(null, result)
    }

    @Test
    fun `savePreferences serializes single and multiple answers`() = runTest {
        val repository = mockk<OnboardingPreferenceRepository>()
        every { repository.preferences } returns MutableStateFlow(emptyList())
        var requestBody = ""
        val service = OnboardingPreferencesService(
            onboardingPreferenceRepository = repository,
            apiClient = createApiClient(
                onRequest = { body ->
                    requestBody = body
                }
            )
        )

        val result = service.savePreferences(
            UserPreferenceFragmentState(
                selectedSingleOptions = mapOf("pace" to "fast"),
                selectedMultipleOptions = mapOf("interests" to linkedSetOf("art", "food"))
            )
        )

        assertTrue(result.isSuccessful)
        assertEquals(
            """{"items":[{"question_key":"pace","answer_key":"fast"},{"question_key":"interests","answer_keys":["art","food"]}]}""",
            requestBody
        )
    }

    @Test
    fun `savePreferences skips empty answers`() = runTest {
        val repository = mockk<OnboardingPreferenceRepository>()
        every { repository.preferences } returns MutableStateFlow(emptyList())
        var requestBody = ""
        val service = OnboardingPreferencesService(
            onboardingPreferenceRepository = repository,
            apiClient = createApiClient(
                onRequest = { body ->
                    requestBody = body
                }
            )
        )

        val result = service.savePreferences(
            UserPreferenceFragmentState(
                selectedSingleOptions = mapOf("pace" to "fast", "mood" to ""),
                selectedMultipleOptions = mapOf(
                    "interests" to linkedSetOf("art", " "),
                    "empty" to emptySet()
                )
            )
        )

        assertTrue(result.isSuccessful)
        assertEquals(
            """{"items":[{"question_key":"pace","answer_key":"fast"},{"question_key":"interests","answer_keys":["art"]}]}""",
            requestBody
        )
    }

    private fun createApiClient(
        status: HttpStatusCode = HttpStatusCode.OK,
        onRequest: (String) -> Unit = {}
    ): ApiClient {
        val appDataRepository = mockk<AppDataRepository>(relaxed = true)
        every { appDataRepository.bearerTokenFlow } returns MutableStateFlow(null)
        coEvery { appDataRepository.shouldRefreshBearerToken() } returns false

        val mockEngine = MockEngine { request ->
            val body = request.body
            if (body is TextContent) {
                onRequest(body.text)
            }

            respond(
                content = Json.encodeToString(EmptyAPIResponse()),
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
