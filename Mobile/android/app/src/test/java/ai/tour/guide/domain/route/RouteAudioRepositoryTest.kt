package ai.tour.guide.domain.route

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class RouteAudioRepositoryTest {

    private lateinit var context: Application
    private lateinit var repository: RouteAudioRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = RouteAudioRepository(context)
    }

    @Test
    fun `appendChunk returns null before session is started`() = runTest {
        assertNull(repository.appendChunk("audio".toByteArray()))
    }

    @Test
    fun `startSession creates cache directory`() = runTest {
        repository.startSession("session-1")

        val dir = File(context.cacheDir, "route_audio_session-1")
        assertTrue(dir.exists())
        assertTrue(dir.isDirectory)
    }

    @Test
    fun `startSession deletes and recreates existing directory`() = runTest {
        repository.startSession("session-1")
        val dir = File(context.cacheDir, "route_audio_session-1")
        val staleFile = File(dir, "old.mp3").apply { createNewFile() }
        assertTrue(staleFile.exists())

        repository.startSession("session-1")

        assertFalse(staleFile.exists())
        assertTrue(dir.exists())
    }

    @Test
    fun `startSession resets chunk index to zero`() = runTest {
        repository.startSession("session-1")
        repository.appendChunk("a".toByteArray())
        repository.appendChunk("b".toByteArray())

        repository.startSession("session-2")
        val file = repository.appendChunk("c".toByteArray())

        assertEquals("chunk_00000.mp3", file?.name)
    }

    @Test
    fun `appendChunk writes data to file and returns the correct File`() = runTest {
        repository.startSession("session-1")
        val data = "audio_data".toByteArray()

        val file = repository.appendChunk(data)

        assertNotNull(file)
        assertTrue(file!!.exists())
        assertArrayEquals(data, file.readBytes())
    }

    @Test
    fun `appendChunk names files sequentially`() = runTest {
        repository.startSession("session-1")

        val f0 = repository.appendChunk("a".toByteArray())
        val f1 = repository.appendChunk("b".toByteArray())
        val f2 = repository.appendChunk("c".toByteArray())

        assertEquals("chunk_00000.mp3", f0?.name)
        assertEquals("chunk_00001.mp3", f1?.name)
        assertEquals("chunk_00002.mp3", f2?.name)
    }

    @Test
    fun `appendChunk returns null after clearSession`() = runTest {
        repository.startSession("session-1")
        repository.clearSession()

        assertNull(repository.appendChunk("audio".toByteArray()))
    }

    @Test
    fun `clearSession deletes session directory`() = runTest {
        repository.startSession("session-1")
        val dir = File(context.cacheDir, "route_audio_session-1")
        assertTrue(dir.exists())

        repository.clearSession()

        assertFalse(dir.exists())
    }

    @Test
    fun `clearSession allows new session to start fresh`() = runTest {
        repository.startSession("session-1")
        repository.appendChunk("a".toByteArray())
        repository.appendChunk("b".toByteArray())
        repository.clearSession()

        repository.startSession("session-2")
        val file = repository.appendChunk("c".toByteArray())

        assertEquals("chunk_00000.mp3", file?.name)
    }
}
