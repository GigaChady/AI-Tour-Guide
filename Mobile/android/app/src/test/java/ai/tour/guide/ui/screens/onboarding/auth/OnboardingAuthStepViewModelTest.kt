package ai.tour.guide.ui.screens.onboarding.auth

import ai.tour.guide.R
import ai.tour.guide.domain.auth.AuthService
import ai.tour.guide.network.ApiBaseResponseResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingAuthStepViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authService: AuthService
    private lateinit var viewModel: OnboardingAuthStepViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authService = mockk()
        viewModel = OnboardingAuthStepViewModel(authService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `performLoginRequest success updates isSuccess`() = runTest {
        val response = mockk<ApiBaseResponseResult>()
        every { response.isSuccessful } returns true
        coEvery { authService.login("test@test.com", "password") } returns response

        viewModel.onEmailChanged("test@test.com")
        viewModel.onPasswordChanged("password")
        advanceUntilIdle()
        viewModel.performLoginRequest()
        advanceUntilIdle()

        assertTrue(viewModel.viewStateFlow.value.isSuccess)
        coVerify(exactly = 1) { authService.login("test@test.com", "password") }
    }

    @Test
    fun `onRegisterClicked password mismatch sets error`() = runTest {
        viewModel.onPasswordChanged("pass1")
        viewModel.onConfirmPasswordChanged("pass2")

        viewModel.onRegisterClicked()
        advanceUntilIdle()

        assertEquals(
            R.string.validation_error_password_mismatch,
            viewModel.viewStateFlow.value.toastMessage
        )
        coVerify(exactly = 0) { authService.register(any(), any(), any()) }
    }
}
