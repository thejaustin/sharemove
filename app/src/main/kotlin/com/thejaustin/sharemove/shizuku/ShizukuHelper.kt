package com.thejaustin.sharemove.shizuku

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import java.io.File

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

    fun runCommand(command: String): Result<String> {
        if (!isAvailable)   return Result.failure(Exception("Shizuku is not running"))
        if (!hasPermission) return Result.failure(Exception("Shizuku permission not granted"))
        return runViaRish(command)
    }

    private fun runViaRish(command: String): Result<String> {
        val candidates = listOf(
            "/data/local/tmp/rish",
            "/data/data/com.termux/files/home/rish",
        )
        for (path in candidates) {
            if (!File(path).exists()) continue
            return try {
                val pb = ProcessBuilder(path, "-c", command)
                pb.environment()["RISH_APPLICATION_ID"] = "com.thejaustin.sharemove"
                val process = pb.start()
                val stdout = process.inputStream.bufferedReader().readText()
                val stderr = process.errorStream.bufferedReader().readText()
                val exit   = process.waitFor()
                if (exit != 0 && stderr.isNotBlank())
                    Result.failure(Exception("rish($exit): $stderr"))
                else
                    Result.success(stdout.trim())
            } catch (e: Exception) { Result.failure(e) }
        }
        return Result.failure(Exception("rish not found — is Shizuku installed?"))
    }
}
