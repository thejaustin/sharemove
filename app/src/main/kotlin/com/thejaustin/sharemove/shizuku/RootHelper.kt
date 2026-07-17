package com.thejaustin.sharemove.shizuku

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RootHelper {

    /**
     * Blocking; call from a background dispatcher.
     *
     * We actually run `su -c id` and check for `uid=0` rather than just testing
     * whether an `su` binary exists on PATH. On Samsung Knox-enforced devices the
     * binary may be present but root access denied; a bare `which su` would give a
     * false positive.
     */
    fun checkAvailable(): Boolean = try {
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
        val output  = process.inputStream.bufferedReader().readText()
        process.waitFor() == 0 && output.contains("uid=0")
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
