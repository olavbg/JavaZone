package com.olavbg.javazone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.room.Room
import com.olavbg.javazone.data.local.AppDatabase
import com.olavbg.javazone.data.remote.SleepingPillApi
import com.olavbg.javazone.data.repository.SessionRepository
import com.olavbg.javazone.data.repository.SettingsRepository
import com.olavbg.javazone.model.BackgroundMode
import com.olavbg.javazone.notifications.ConferenceDoneReceiver
import com.olavbg.javazone.notifications.ReminderManager
import com.olavbg.javazone.ui.JavaZoneApp
import com.olavbg.javazone.ui.components.LocalBackgroundReanimate
import com.olavbg.javazone.ui.theme.JavaZoneTheme
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
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
            CompositionLocalProvider(
                LocalBackgroundReanimate provides reanimateSignal
            ) {
                JavaZoneTheme(backgroundMode = backgroundMode) {
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
