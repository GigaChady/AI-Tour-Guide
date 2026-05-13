package ai.tour.guide.ui.screens.main.appSettings

import ai.tour.guide.data.appSettings.AppSettingsAppThemeType
import ai.tour.guide.data.appSettings.AppSettingsDetailLevelType
import ai.tour.guide.domain.appSettings.AppSettingsService
import ai.tour.guide.network.rest.ApiBaseResponseResult
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
class AppSettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var service: AppSettingsService
    private lateinit var viewModel: AppSettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        service = mockk()
        viewModel = AppSettingsViewModel(service)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onStart fetches settings and applies returned state`() = runTest {
        val fetched = AppSettingsState(language = "pl", appTheme = AppSettingsAppThemeType.DARK)
        coEvery { service.fetchSettingsIfEmpty() } returns fetched

        viewModel.onStart()
        advanceUntilIdle()

        assertEquals(fetched, viewModel.viewStateFlow.value.data)
    }

    @Test
    fun `onStart does not fetch again on second call`() = runTest {
        coEvery { service.fetchSettingsIfEmpty() } returns null

        viewModel.onStart()
        viewModel.onStart()
        advanceUntilIdle()

        coVerify(exactly = 1) { service.fetchSettingsIfEmpty() }
    }

    @Test
    fun `updateLanguage updates state`() = runTest {
        viewModel.updateLanguage("pl")
        advanceUntilIdle()
        assertEquals("pl", viewModel.viewStateFlow.value.data.language)
    }

    @Test
    fun `updateTheme updates state`() = runTest {
        viewModel.updateTheme(AppSettingsAppThemeType.DARK)
        advanceUntilIdle()
        assertEquals(AppSettingsAppThemeType.DARK, viewModel.viewStateFlow.value.data.appTheme)
    }

    @Test
    fun `updatePitch updates state`() = runTest {
        viewModel.updatePitch(75f)
        advanceUntilIdle()
        assertEquals(75f, viewModel.viewStateFlow.value.data.pitch)
    }

    @Test
    fun `updateSpeed updates state`() = runTest {
        viewModel.updateSpeed(9f)
        advanceUntilIdle()
        assertEquals(9f, viewModel.viewStateFlow.value.data.speed)
    }

    @Test
    fun `updateDetailLevel updates state`() = runTest {
        viewModel.updateDetailLevel(AppSettingsDetailLevelType.HIGH)
        advanceUntilIdle()
        assertEquals(AppSettingsDetailLevelType.HIGH, viewModel.viewStateFlow.value.data.detailLevel)
    }

    @Test
    fun `updateAutoPlay updates state`() = runTest {
        viewModel.updateAutoPlay(false)
        advanceUntilIdle()
        assertEquals(false, viewModel.viewStateFlow.value.data.autoPlay)
    }

    @Test
    fun `onSaveSettingsClicked success sets isSuccess flag`() = runTest {
        val response = mockk<ApiBaseResponseResult>()
        every { response.isSuccessful } returns true
        coEvery { service.saveSettings(any()) } returns response

        viewModel.onSaveSettingsClicked()
        advanceUntilIdle()

        assertTrue(viewModel.viewStateFlow.value.isSuccess)
    }

    @Test
    fun `onSaveSettingsClicked failure sets toast message`() = runTest {
        val response = mockk<ApiBaseResponseResult>()
        every { response.isSuccessful } returns false
        every { response.errorMessage } returns "Server Error"
        coEvery { service.saveSettings(any()) } returns response

        viewModel.onSaveSettingsClicked()
        advanceUntilIdle()

        assertEquals("Server Error", viewModel.viewStateFlow.value.toastMessage.toString())
    }

    @Test
    fun `clearError sets toastMessage to null`() = runTest {
        val response = mockk<ApiBaseResponseResult>()
        every { response.isSuccessful } returns false
        every { response.errorMessage } returns "error"
        coEvery { service.saveSettings(any()) } returns response
        viewModel.onSaveSettingsClicked()
        advanceUntilIdle()

        viewModel.clearError()
        advanceUntilIdle()

        assertEquals(null, viewModel.viewStateFlow.value.toastMessage)
    }
}
