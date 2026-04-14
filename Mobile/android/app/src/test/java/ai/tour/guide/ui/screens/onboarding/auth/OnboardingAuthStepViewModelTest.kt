package ai.tour.guide.ui.screens.onboarding.auth

import ai.tour.guide.R
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
class OnboardingAuthStepViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var appStateRepository: AppDataRepository
    private lateinit var viewModel: OnboardingAuthStepViewModel

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
        every { appStateRepository.bearerTokenFlow } returns MutableStateFlow(null)

        return ApiClient(appStateRepository, httpClient)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        appStateRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onLoginClicked success updates isSuccess and credentials`() = runTest {
        val tokenResponse = TokenResponseDto(accessToken = "token")
        val json = Json.encodeToString(tokenResponse)
        viewModel = OnboardingAuthStepViewModel(createApiClient(json), appStateRepository)

        viewModel.onEmailChanged("test@test.com")
        viewModel.onPasswordChanged("password")
        viewModel.performLoginRequest()
        advanceUntilIdle()

        assertTrue(viewModel.viewStateFlow.value.isSuccess)
        coVerify { appStateRepository.updateCredentialsWithAPIResponse(any()) }
    }

    @Test
    fun `onRegisterClicked password mismatch sets error`() = runTest {
        viewModel = OnboardingAuthStepViewModel(createApiClient(), appStateRepository)
        viewModel.onPasswordChanged("pass1")
        viewModel.onConfirmPasswordChanged("pass2")

        viewModel.onRegisterClicked()
        advanceUntilIdle()

        assertEquals(
            R.string.validation_error_password_mismatch,
            viewModel.viewStateFlow.value.errorMessage
        )
    }
}
