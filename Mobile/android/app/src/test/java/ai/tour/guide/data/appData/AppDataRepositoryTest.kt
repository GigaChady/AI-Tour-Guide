package ai.tour.guide.data.appData

import ai.tour.guide.network.schema.response.TokenResponseDto
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AppDataRepositoryTest {

    private lateinit var repository: AppDataRepository

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Application>()
        repository = AppDataRepository(context)
        // Ensure clean state per test because DataStore instance might be shared in JVM
        repository.updateOnboardingCompleted(false)
    }

    @Test
    fun `updateCredentialsWithAPIResponse updates bearer token`() = runTest {
        val response = TokenResponseDto(accessToken = "access", refreshToken = "refresh")
        repository.updateCredentialsWithAPIResponse(response)

        assertEquals("access", repository.bearerTokenFlow.value)
    }

    @Test
    fun `updateOnboardingCompleted persists and updates flow`() = runTest {
        repository.updateOnboardingCompleted(true)
        val completed = repository.onboardingCompletedFlow.first()
        assertEquals(true, completed)
    }

    @Test
    fun `onboardingCompleted defaults to false`() = runTest {
        // This relies on setUp resetting it to false
        val completed = repository.onboardingCompletedFlow.first()
        assertEquals(false, completed)
    }
}
