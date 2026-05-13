package ai.tour.guide.data.appSettings

import ai.tour.guide.network.schema.response.AppSettingsResponseDto
import ai.tour.guide.network.schema.response.toDto
import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsMappersTest {

    @Test
    fun `toState maps all present fields`() {
        val dto = AppSettingsDto(
            appTheme = AppSettingsAppThemeType.DARK,
            language = "pl",
            pitch = 75,
            speed = 9,
            detailLevel = AppSettingsDetailLevelType.HIGH,
            autoPlay = false
        )

        val state = dto.toState()

        assertEquals(AppSettingsAppThemeType.DARK, state.appTheme)
        assertEquals("pl", state.language)
        assertEquals(75f, state.pitch)
        assertEquals(9f, state.speed)
        assertEquals(AppSettingsDetailLevelType.HIGH, state.detailLevel)
        assertEquals(false, state.autoPlay)
    }

    @Test
    fun `toState applies defaults for all-null dto`() {
        val state = AppSettingsDto().toState()

        assertEquals(AppSettingsAppThemeType.SYSTEM, state.appTheme)
        assertEquals("en", state.language)
        assertEquals(50f, state.pitch)
        assertEquals(5f, state.speed)
        assertEquals(AppSettingsDetailLevelType.MEDIUM, state.detailLevel)
        assertEquals(true, state.autoPlay)
    }

    @Test
    fun `toDto injects provided theme into dto`() {
        val response = AppSettingsResponseDto(language = "en")

        val dto = response.toDto(AppSettingsAppThemeType.DARK)

        assertEquals(AppSettingsAppThemeType.DARK, dto.appTheme)
    }

    @Test
    fun `toDto passes null theme through to dto`() {
        val response = AppSettingsResponseDto(language = "en")

        val dto = response.toDto(null)

        assertEquals(null, dto.appTheme)
    }
}
