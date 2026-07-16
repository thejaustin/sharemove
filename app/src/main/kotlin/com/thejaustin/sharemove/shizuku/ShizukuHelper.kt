package com.thejaustin.sharemove.shizuku

import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

object ShizukuHelper {

    val isAvailable: Boolean
        get() = try { Shizuku.pingBinder() } catch (_: Exception) { false }

    val hasPermission: Boolean
        get() = try {
            !Shizuku.isPreV11() &&
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) { false }

    fun requestPermission(requestCode: Int) {
        try { if (!Shizuku.isPreV11()) Shizuku.requestPermission(requestCode) }
        catch (_: Exception) { }
    }

    /** Run a shell command as the Shizuku identity (shell uid). */
    suspend fun runCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isAvailable)   return@withContext Result.failure(Exception("Shizuku is not running"))
        if (!hasPermission) return@withContext Result.failure(Exception("Shizuku permission not granted"))
        try {
            val process = newProcess(arrayOf("sh", "-c", command))
            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()
            val exit   = process.waitFor()
            if (exit == 0)
                Result.success(stdout.trim())
            else
                Result.failure(Exception("shizuku($exit): ${stderr.ifBlank { stdout }.trim()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Shizuku.newProcess is private API but the de-facto stable way to spawn a remote
    // shell; proguard-rules.pro keeps all of rikka.shizuku so reflection survives R8.
    private fun newProcess(cmd: Array<String>): Process {
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java,
        )
        method.isAccessible = true
        return method.invoke(null, cmd, null, null) as Process
    }
}
