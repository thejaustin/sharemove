package com.thejaustin.sharemove.shizuku

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RootHelper {

    /** Blocking; call from a background dispatcher. */
    fun checkAvailable(): Boolean = try {
        Runtime.getRuntime().exec(arrayOf("which", "su")).waitFor() == 0
    } catch (_: Exception) { false }

    suspend fun runCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()
            val exit   = process.waitFor()
            if (exit == 0)
                Result.success(stdout.trim())
            else
                Result.failure(Exception("root($exit): ${stderr.ifBlank { stdout }.trim()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
