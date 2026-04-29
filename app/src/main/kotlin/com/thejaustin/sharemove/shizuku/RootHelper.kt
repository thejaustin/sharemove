package com.thejaustin.sharemove.shizuku

import java.io.DataOutputStream

object RootHelper {

    val isAvailable: Boolean
        get() = try {
            Runtime.getRuntime().exec("which su").waitFor() == 0
        } catch (_: Exception) { false }

    fun runCommand(command: String): Result<String> = try {
        val process = Runtime.getRuntime().exec("su")
        DataOutputStream(process.outputStream).use { os ->
            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()
        }
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val exit   = process.waitFor()
        if (exit != 0 && stderr.isNotBlank())
            Result.failure(Exception("root($exit): $stderr"))
        else
            Result.success(stdout.trim())
    } catch (e: Exception) { Result.failure(e) }
}
