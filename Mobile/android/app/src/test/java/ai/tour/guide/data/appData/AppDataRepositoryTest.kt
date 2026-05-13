package ai.tour.guide.data.appData

import ai.tour.guide.data.appSettings.AppSettingsAppThemeType
import ai.tour.guide.network.schema.response.TokenResponseDto
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
        repository.clearSessionData()
    }

    @Test
    fun `updateCredentialsWithAPIResponse updates bearer token`() = runTest {
        val response = TokenResponseDto(accessToken = "access", refreshToken = "refresh")
        repository.updateCredentialsWithAPIResponse(response)

        assertEquals("access", repository.bearerTokenFlow.value)
    }

    @Test
    fun `updateCredentialsWithAPIResponse with null is a no-op`() = runTest {
        repository.updateCredentialsWithAPIResponse(null)

        assertNull(repository.bearerTokenFlow.value)
    }

    @Test
    fun `updateBearerToken with null is a no-op`() = runTest {
        repository.updateBearerToken(null)

        assertNull(repository.bearerTokenFlow.value)
    }

    @Test
    fun `updateOnboardingCompleted persists and updates flow`() = runTest {
        repository.updateOnboardingCompleted(true)
        val completed = repository.onboardingCompletedFlow.first()
        assertEquals(true, completed)
    }

    @Test
    fun `onboardingCompleted defaults to false`() = runTest {
        val completed = repository.onboardingCompletedFlow.first()
        assertEquals(false, completed)
    }

    @Test
    fun `clearSessionData resets bearer token and onboarding flag`() = runTest {
        repository.updateCredentialsWithAPIResponse(TokenResponseDto("access", "refresh"))
        repository.updateOnboardingCompleted(true)

        repository.clearSessionData()

        assertNull(repository.bearerTokenFlow.value)
        assertFalse(repository.onboardingCompletedFlow.first())
    }

    @Test
    fun `getRefreshToken returns stored token`() = runTest {
        repository.updateCredentialsWithAPIResponse(TokenResponseDto("access", "my-refresh-token"))

        val token = repository.getRefreshToken()

        assertEquals("my-refresh-token", token)
    }

    @Test
    fun `getRefreshToken clears session and returns empty string when token is null`() = runTest {
        repository.updateOnboardingCompleted(true)

        val token = repository.getRefreshToken()

        assertEquals("", token)
        assertFalse(repository.onboardingCompletedFlow.first())
    }

    @Test
    fun `shouldRefreshBearerToken returns true when token missing and onboarding complete`() = runTest {
        repository.updateOnboardingCompleted(true)

        assertTrue(repository.shouldRefreshBearerToken())
    }

    @Test
    fun `shouldRefreshBearerToken returns false when bearer token is already set`() = runTest {
        repository.updateOnboardingCompleted(true)
        repository.updateCredentialsWithAPIResponse(TokenResponseDto("access", "refresh"))

        assertFalse(repository.shouldRefreshBearerToken())
    }

    @Test
    fun `shouldRefreshBearerToken returns false when onboarding not complete`() = runTest {
        assertFalse(repository.shouldRefreshBearerToken())
    }

    @Test
    fun `updateRefreshToken persists token readable via getRefreshToken`() = runTest {
        repository.updateRefreshToken("direct-token")

        assertEquals("direct-token", repository.getRefreshToken())
    }

    @Test
    fun `updateAppTheme persists and is readable via appThemeFlow`() = runTest {
        repository.updateAppTheme(AppSettingsAppThemeType.DARK)

        assertEquals(AppSettingsAppThemeType.DARK, repository.appThemeFlow.first())
    }
}
