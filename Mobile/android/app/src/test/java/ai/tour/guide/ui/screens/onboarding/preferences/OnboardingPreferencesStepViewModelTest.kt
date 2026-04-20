package ai.tour.guide.ui.screens.onboarding.preferences

import ai.tour.guide.domain.preferences.OnboardingPreferencesService
import ai.tour.guide.network.ApiBaseResponseResult
import ai.tour.guide.ui.sharedFragments.preferences.UserPreferenceFragmentState
import ai.tour.guide.ui.sharedFragments.preferences.UserPreferenceFragmentViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingPreferencesStepViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var service: OnboardingPreferencesService
    private lateinit var viewModel: UserPreferenceFragmentViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        service = mockk()
        every { service.preferences } returns MutableStateFlow(emptyList())
        viewModel = UserPreferenceFragmentViewModel(service)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onOptionSelected updates state`() = runTest {
        viewModel.onOptionSelected("q1", "a1")
        advanceUntilIdle()

        assertEquals("a1", viewModel.viewStateFlow.value.data.selectedSingleOptions["q1"])
    }

    @Test
    fun `onOptionSelected does not autosave by default`() = runTest {
        viewModel.onOptionSelected("q1", "a1")
        advanceUntilIdle()

        coVerify(exactly = 0) { service.savePreferences(any()) }
    }

    @Test
    fun `onStart fetches preferences once and applies fetched state`() = runTest {
        val fetchedState = UserPreferenceFragmentState(
            selectedSingleOptions = mapOf("gender" to "female"),
            selectedMultipleOptions = mapOf("interests" to setOf("history"))
        )
        coEvery { service.fetchPreferencesIfEmpty() } returns fetchedState

        viewModel.onStart()
        viewModel.onStart()
        advanceUntilIdle()

        coVerify(exactly = 1) { service.fetchPreferencesIfEmpty() }
        assertEquals(fetchedState, viewModel.viewStateFlow.value.data)
    }

    @Test
    fun `savePreferences success updates isSuccess`() = runTest {
        val response = mockk<ApiBaseResponseResult>()
        every { response.isSuccessful } returns true
        coEvery { service.savePreferences(any()) } returns response

        viewModel.onOptionSelected("q1", "a1")
        viewModel.savePreferences()
        advanceUntilIdle()

        assertTrue(viewModel.viewStateFlow.value.isSuccess)
    }

    @Test
    fun `savePreferences failure updates errorMessage`() = runTest {
        val response = mockk<ApiBaseResponseResult>()
        every { response.isSuccessful } returns false
        every { response.errorMessage } returns "Error Message"
        coEvery { service.savePreferences(any()) } returns response

        viewModel.onOptionSelected("q1", "a1")
        viewModel.savePreferences()
        advanceUntilIdle()

        assertEquals("Error Message", viewModel.viewStateFlow.value.toastMessage.toString())
    }
}
