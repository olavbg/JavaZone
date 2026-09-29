package com.olavbg.javazone.crash

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import com.olavbg.javazone.BuildConfig
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

class CrashHandler private constructor(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val report = buildCrashReport(throwable)
            val intent = Intent(context, CrashActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(CrashActivity.EXTRA_CRASH_REPORT, report)
                putExtra(CrashActivity.EXTRA_EXCEPTION_NAME, throwable.javaClass.simpleName)
            }
            context.startActivity(intent)

            Process.killProcess(Process.myPid())
            exitProcess(10)
        } catch (t: Throwable) {
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun buildCrashReport(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        val stackTrace = sw.toString().trim()

        return buildString {
            appendLine("### Environment")
            appendLine("- App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("- Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("- Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine()
            appendLine("### Exception")
            appendLine(throwable.toString())
            appendLine()
            appendLine("### Stacktrace")
            appendLine("```")
            appendLine(stackTrace)
            appendLine("```")
        }
    }

    companion object {
        fun install(context: Context) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            if (defaultHandler !is CrashHandler) {
                Thread.setDefaultUncaughtExceptionHandler(
                    CrashHandler(context.applicationContext, defaultHandler)
                )
            }
        }
    }
}
