package com.olavbg.javazone

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import androidx.activity.SystemBarStyle
import com.olavbg.javazone.data.local.AppDatabase
import com.olavbg.javazone.data.remote.SleepingPillApi
import com.olavbg.javazone.data.repository.SessionRepository
import com.olavbg.javazone.data.repository.SettingsRepository
import com.olavbg.javazone.model.BackgroundMode
import com.olavbg.javazone.model.ThemeMode
import com.olavbg.javazone.notifications.ConferenceDoneReceiver
import com.olavbg.javazone.notifications.ReminderManager
import com.olavbg.javazone.ui.JavaZoneApp
import com.olavbg.javazone.ui.components.LocalBackgroundReanimate
import com.olavbg.javazone.ui.theme.JavaZoneTheme
import com.olavbg.javazone.ui.timeline.TimelineViewModel
import com.olavbg.javazone.ui.timeline.TimelineViewModelFactory
import com.olavbg.javazone.util.AppLocale
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class MainActivity : ComponentActivity() {

    // On API < 33 the per-app language override is applied by re-creating the
    // activity through a localized base context (see AppLocale). On API 33+ the
    // system LocaleManager owns this and a wrapping would fight it.
    override fun attachBaseContext(newBase: Context) {
        val base = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            newBase
        } else {
            AppLocale.localizedContext(newBase)
        }
        super.attachBaseContext(base)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Tie the system splash to the real boot path: it must be installed before
        // super.onCreate() so the starting theme (with the animated icon) is applied,
        // then kept on screen only while data is actually loading.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "javazone.db")
            .addMigrations(AppDatabase.MIGRATION_3_4)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(SleepingPillApi.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        val api = retrofit.create(SleepingPillApi::class.java)
        
        val settingsRepository = SettingsRepository(this)
        val reminderManager = ReminderManager(this)
        val repository = SessionRepository(api, db.sessionDao(), reminderManager, settingsRepository)

        // Create the shared TimelineViewModel here (same store/key as JavaZoneApp uses), so
        // its isLoading flag can drive the splash and the initial load starts early.
        val timelineViewModel: TimelineViewModel = ViewModelProvider(
            this,
            TimelineViewModelFactory(repository, settingsRepository)
        )[TimelineViewModel::class.java]

        // Hold the system splash while data is actually loading, with a hard 3 s cap
        // so a broken network never blocks the app from starting.
        val startedAt = SystemClock.uptimeMillis()
        splashScreen.setKeepOnScreenCondition {
            timelineViewModel.isLoading.value && SystemClock.uptimeMillis() - startedAt < 3000L
        }

        val initialSessionId = intent.getStringExtra("session_id")
        val initialShowDonation = intent.getBooleanExtra(
            ConferenceDoneReceiver.EXTRA_SHOW_DONATION_DIALOG,
            false
        )

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContent {
            var reanimateSignal by remember { mutableLongStateOf(0L) }
            val backgroundMode by settingsRepository.backgroundMode
                .collectAsState(initial = BackgroundMode.Animated)
            val themeMode by settingsRepository.themeMode
                .collectAsState(initial = ThemeMode.Dark)
            val darkTheme = themeMode.resolveDark(isSystemInDarkTheme())

            // The app no longer follows the phone's theme by default, so the system bar
            // icon colors must follow the resolved in-app theme instead of the OS theme.
            LaunchedEffect(darkTheme) {
                val style = if (darkTheme) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(
                        scrim = android.graphics.Color.TRANSPARENT,
                        darkScrim = android.graphics.Color.TRANSPARENT
                    )
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            CompositionLocalProvider(
                LocalBackgroundReanimate provides reanimateSignal
            ) {
                JavaZoneTheme(darkTheme = darkTheme, backgroundMode = backgroundMode) {
                    JavaZoneApp(
                        repository,
                        settingsRepository,
                        reminderManager,
                        initialSessionId,
                        initialShowDonation,
                        onNavigation = { reanimateSignal++ }
                    )
                }
            }
        }
    }
}
