package ai.tour.guide.domain.route

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.io.File
import java.io.FileOutputStream

@Single
class RouteAudioRepository(private val context: Context) {
    private val lock = Any()

    private var sessionDir: File? = null
    private var nextChunkIndex: Int = 0
    private val chunkFiles = mutableListOf<File>()

    suspend fun startSession(sessionId: String) {
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                val directory = File(context.cacheDir, "route_audio_$sessionId")
                directory.deleteRecursively()
                directory.mkdirs()
                sessionDir = directory
                nextChunkIndex = 0
                chunkFiles.clear()
            }
        }
    }

    suspend fun appendChunk(chunk: ByteArray): File? {
        return withContext(Dispatchers.IO) {
            synchronized(lock) {
                val directory = sessionDir ?: return@withContext null
                val chunkFile = File(directory, chunkFileName(nextChunkIndex++))
                FileOutputStream(chunkFile).use { output ->
                    output.write(chunk)
                    output.flush()
                }
                chunkFiles.add(chunkFile)
                chunkFile
            }
        }
    }
    
    suspend fun clearSession() {
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                sessionDir?.deleteRecursively()
                sessionDir = null
                nextChunkIndex = 0
                chunkFiles.clear()
            }
        }
    }

    private fun chunkFileName(index: Int): String {
        return "chunk_%05d.mp3".format(index)
    }
}
