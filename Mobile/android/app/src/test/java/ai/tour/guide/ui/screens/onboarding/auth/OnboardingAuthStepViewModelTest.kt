package ai.tour.guide.ui.screens.onboarding.auth

import ai.tour.guide.R
import ai.tour.guide.domain.auth.AuthService
import ai.tour.guide.network.rest.ApiBaseResponseResult
import ai.tour.guide.network.schema.response.MeParamsResponseDto
import android.content.Context
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
import org.junit.Assert.assertNotNull
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

    @Test
    fun `onNameChanged updates name in state`() = runTest {
        viewModel.onNameChanged("Alice")
        advanceUntilIdle()
        assertEquals("Alice", viewModel.viewStateFlow.value.data.name)
    }

    @Test
    fun `onEmailChanged updates email in state`() = runTest {
        viewModel.onEmailChanged("alice@example.com")
        advanceUntilIdle()
        assertEquals("alice@example.com", viewModel.viewStateFlow.value.data.email)
    }

    @Test
    fun `onPasswordChanged updates password in state`() = runTest {
        viewModel.onPasswordChanged("secret123")
        advanceUntilIdle()
        assertEquals("secret123", viewModel.viewStateFlow.value.data.password)
    }

    @Test
    fun `onConfirmPasswordChanged updates confirmPassword in state`() = runTest {
        viewModel.onConfirmPasswordChanged("secret123")
        advanceUntilIdle()
        assertEquals("secret123", viewModel.viewStateFlow.value.data.confirmPassword)
    }

    @Test
    fun `onLoginClicked calls authService login`() = runTest {
        val response = mockk<ApiBaseResponseResult>()
        every { response.isSuccessful } returns true
        coEvery { authService.login(any(), any()) } returns response

        viewModel.onLoginClicked()
        advanceUntilIdle()

        coVerify(exactly = 1) { authService.login(any(), any()) }
    }

    @Test
    fun `onRegisterClicked success calls authService register and updates isSuccess`() = runTest {
        val response = mockk<ApiBaseResponseResult>()
        every { response.isSuccessful } returns true
        coEvery { authService.register(any(), any(), any()) } returns response

        viewModel.onPasswordChanged("samepass")
        viewModel.onConfirmPasswordChanged("samepass")
        viewModel.onRegisterClicked()
        advanceUntilIdle()

        assertTrue(viewModel.viewStateFlow.value.isSuccess)
        coVerify(exactly = 1) { authService.register(any(), any(), any()) }
    }

    @Test
    fun `onAccountSettingsViewLoaded loads user data into state`() = runTest {
        coEvery { authService.loadCurrentUserData() } returns MeParamsResponseDto(
            email = "bob@example.com",
            name = "Bob"
        )

        viewModel.onAccountSettingsViewLoaded()
        advanceUntilIdle()

        assertEquals("bob@example.com", viewModel.viewStateFlow.value.data.email)
        assertEquals("Bob", viewModel.viewStateFlow.value.data.name)
    }

    @Test
    fun `onSignInWithGoogleClicked success updates isSuccess`() = runTest {
        val context = mockk<Context>(relaxed = true)
        val response = mockk<ApiBaseResponseResult>()
        every { response.isSuccessful } returns true
        coEvery { authService.signInWithGoogle(any()) } returns response

        viewModel.onSignInWithGoogleClicked(context)
        advanceUntilIdle()

        assertTrue(viewModel.viewStateFlow.value.isSuccess)
    }

    @Test
    fun `onSignInWithGoogleClicked failure sets toast message`() = runTest {
        val context = mockk<Context>(relaxed = true)
        val response = mockk<ApiBaseResponseResult>()
        every { response.isSuccessful } returns false
        every { response.errorMessage } returns "Google sign-in failed"
        coEvery { authService.signInWithGoogle(any()) } returns response

        viewModel.onSignInWithGoogleClicked(context)
        advanceUntilIdle()

        assertNotNull(viewModel.viewStateFlow.value.toastMessage)
    }
}
