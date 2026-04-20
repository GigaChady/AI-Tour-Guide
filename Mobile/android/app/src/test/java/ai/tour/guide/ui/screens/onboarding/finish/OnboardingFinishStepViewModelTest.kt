package ai.tour.guide.ui.screens.onboarding.finish

import ai.tour.guide.data.appData.AppDataRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingFinishStepViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val appDataRepository: AppDataRepository = mockk(relaxed = true)
    private lateinit var viewModel: OnboardingFinishStepViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = OnboardingFinishStepViewModel(appDataRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onFinishClicked updates repository and state`() = runTest {
        viewModel.onFinishClicked()
        advanceUntilIdle()
        
        assertTrue(viewModel.completedStateFlow.value)
        coVerify { appDataRepository.updateOnboardingCompleted(true) }
    }
}
