package ai.tour.guide.ui.screens.onboarding.preferences

import ai.tour.guide.data.appData.AppDataRepository
import ai.tour.guide.data.onboardingPreferences.OnboardingPreferenceRepository
import ai.tour.guide.network.ApiClient
import ai.tour.guide.network.schema.response.EmptyAPIResponse
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingPreferencesStepViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: OnboardingPreferenceRepository
    private lateinit var viewModel: OnboardingPreferencesStepViewModel

    private fun createApiClient(
        jsonResponse: String = "{}",
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
        val appDataRepository = mockk<AppDataRepository>(relaxed = true)
        every { appDataRepository.bearerTokenFlow } returns MutableStateFlow(null)

        return ApiClient(appDataRepository, httpClient)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        every { repository.preferences } returns MutableStateFlow(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onOptionSelected updates state`() = runTest {
        viewModel = OnboardingPreferencesStepViewModel(repository, createApiClient())
        viewModel.onOptionSelected("q1", "a1")
        advanceUntilIdle()

        assertEquals("a1", viewModel.viewStateFlow.value.data.selectedSingleOptions["q1"])
    }

    @Test
    fun `savePreferences success updates isSuccess`() = runTest {
        val json = Json.encodeToString(EmptyAPIResponse())
        viewModel = OnboardingPreferencesStepViewModel(repository, createApiClient(json))

        viewModel.onOptionSelected("q1", "a1")
        viewModel.savePreferences()
        advanceUntilIdle()

        assertTrue("Expected isSuccess to be true", viewModel.viewStateFlow.value.isSuccess)
    }

    @Test
    fun `savePreferences failure updates errorMessage`() = runTest {
        val json = "{\"detail\": \"Error Message\"}"
        viewModel = OnboardingPreferencesStepViewModel(
            repository,
            createApiClient(json, HttpStatusCode.InternalServerError)
        )

        viewModel.onOptionSelected("q1", "a1")
        viewModel.savePreferences()
        advanceUntilIdle()

        assertEquals("Error Message", viewModel.viewStateFlow.value.errorMessage.toString())
    }
}
