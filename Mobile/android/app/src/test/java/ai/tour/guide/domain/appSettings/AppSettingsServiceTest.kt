package ai.tour.guide.domain.appSettings

import ai.tour.guide.data.appData.AppDataRepository
import ai.tour.guide.data.appSettings.AppSettingsAppThemeType
import ai.tour.guide.data.appSettings.AppSettingsDetailLevelType
import ai.tour.guide.data.appSettings.AppSettingsRepository
import ai.tour.guide.network.rest.ApiClient
import ai.tour.guide.network.schema.response.AppSettingsResponseDto
import ai.tour.guide.network.schema.response.EmptyAPIResponse
import ai.tour.guide.ui.screens.main.appSettings.AppSettingsState
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsServiceTest {

    private fun createApiClient(
        status: HttpStatusCode = HttpStatusCode.OK,
        onRequest: (String) -> Unit = {}
    ): ApiClient {
        val appDataRepository = mockk<AppDataRepository>(relaxed = true)
        every { appDataRepository.bearerTokenFlow } returns MutableStateFlow(null)
        coEvery { appDataRepository.shouldRefreshBearerToken() } returns false

        val mockEngine = MockEngine { request ->
            val body = request.body
            if (body is TextContent) onRequest(body.text)
            respond(
                content = Json.encodeToString(EmptyAPIResponse()),
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return ApiClient(appDataRepository, httpClient)
    }

    private fun createService(
        serverResponse: AppSettingsResponseDto? = null,
        localTheme: AppSettingsAppThemeType = AppSettingsAppThemeType.SYSTEM,
        apiStatus: HttpStatusCode = HttpStatusCode.OK,
        onRequest: (String) -> Unit = {}
    ): Triple<AppSettingsService, AppSettingsRepository, AppDataRepository> {
        val repository = mockk<AppSettingsRepository>()
        coEvery { repository.fetchSettingsIfEmpty() } returns serverResponse

        val appDataRepository = mockk<AppDataRepository>(relaxed = true)
        every { appDataRepository.appThemeFlow } returns flowOf(localTheme)

        val service = AppSettingsService(repository, appDataRepository, createApiClient(apiStatus, onRequest))
        return Triple(service, repository, appDataRepository)
    }

    @Test
    fun `fetchSettingsIfEmpty returns null when repository returns null`() = runTest {
        val (service) = createService(serverResponse = null)
        assertNull(service.fetchSettingsIfEmpty())
    }

    @Test
    fun `fetchSettingsIfEmpty merges local DARK theme into returned AppSettingsState`() = runTest {
        val serverResponse = AppSettingsResponseDto(language = "pl", pitch = 60, speed = 8)
        val (service) = createService(serverResponse = serverResponse, localTheme = AppSettingsAppThemeType.DARK)

        val state = service.fetchSettingsIfEmpty()

        assertEquals(AppSettingsAppThemeType.DARK, state?.appTheme)
        assertEquals("pl", state?.language)
        assertEquals(60f, state?.pitch)
    }

    @Test
    fun `fetchSettingsIfEmpty returns null when repository has no new data`() = runTest {
        val (service) = createService(serverResponse = null)
        assertNull(service.fetchSettingsIfEmpty())
    }

    @Test
    fun `saveSettings persists app theme locally via appDataRepository`() = runTest {
        val (service, _, appDataRepository) = createService()

        service.saveSettings(AppSettingsState(appTheme = AppSettingsAppThemeType.DARK))

        coVerify(exactly = 1) { appDataRepository.updateAppTheme(AppSettingsAppThemeType.DARK) }
    }

    @Test
    fun `saveSettings posts correct serialized body`() = runTest {
        var requestBody = ""
        val (service) = createService(onRequest = { requestBody = it })

        service.saveSettings(AppSettingsState(
            language = "pl",
            pitch = 60f,
            speed = 8f,
            detailLevel = AppSettingsDetailLevelType.HIGH,
            autoPlay = false
        ))

        assertTrue(requestBody.contains("\"language\":\"pl\""))
        assertTrue(requestBody.contains("\"pitch\":60"))
        assertTrue(requestBody.contains("\"speed\":8"))
        assertTrue(requestBody.contains("\"detail_level\":\"high\""))
        assertTrue(requestBody.contains("\"auto_play\":false"))
    }

    @Test
    fun `saveSettings returns success on 200`() = runTest {
        val (service) = createService(apiStatus = HttpStatusCode.OK)
        assertTrue(service.saveSettings(AppSettingsState()).isSuccessful)
    }

    @Test
    fun `saveSettings returns failure on 400`() = runTest {
        val (service) = createService(apiStatus = HttpStatusCode.BadRequest)
        assertTrue(!service.saveSettings(AppSettingsState()).isSuccessful)
    }

    @Test
    fun `saveSettings still calls updateAppTheme even when API returns error`() = runTest {
        val (service, _, appDataRepository) = createService(apiStatus = HttpStatusCode.InternalServerError)

        service.saveSettings(AppSettingsState(appTheme = AppSettingsAppThemeType.LIGHT))

        coVerify(exactly = 1) { appDataRepository.updateAppTheme(AppSettingsAppThemeType.LIGHT) }
    }
}
