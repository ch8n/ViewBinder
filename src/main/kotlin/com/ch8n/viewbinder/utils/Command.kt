package com.ch8n.viewbinder.utils

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

object Command {


    @Throws
    fun execute(command: String): String = execute(command, isLivePrint = false, isSkipException = false)

    @Throws
    fun execute(command: String, isLivePrint: Boolean, isSkipException: Boolean): String =
        execute(arrayOf(command), isLivePrint, isSkipException).joinToString(separator = "\n")


    @Throws(IOException::class)
    fun execute(commands: Array<String>, isLivePrint: Boolean, isSkipException: Boolean): List<String> {

        val rt = Runtime.getRuntime()
        val proc = rt.exec(
            arrayOf(
                "/bin/sh", "-c", *commands
            )
        )

        val stdInput = BufferedReader(InputStreamReader(proc.inputStream))
        val stdError = BufferedReader(InputStreamReader(proc.errorStream))

        // Read the output from the command
        var s: String?
        val result = mutableListOf<String>()
        while (stdInput.readLine().also { s = it } != null) {
            if (isLivePrint) {
                println(s)
            }
            result.add(s!!)
        }

        // Read any errors from the attempted command
        val error = StringBuilder()
        while (stdError.readLine().also { s = it } != null) {
            if (isLivePrint) {
                println(s)
            }
            error.append(s).append("\n")
        }

        if (!isSkipException) {
            if (error.isNotBlank()) {
                // has error
                throw IOException(error.toString())
            }
        }

        return result
    }
}