package com.olavbg.javazone

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.olavbg.javazone.data.local.AppDatabase
import com.olavbg.javazone.data.remote.SleepingPillApi
import com.olavbg.javazone.data.repository.SessionRepository
import com.olavbg.javazone.data.repository.SettingsRepository
import com.olavbg.javazone.notifications.ConferenceDoneReceiver
import com.olavbg.javazone.notifications.ReminderManager
import com.olavbg.javazone.ui.JavaZoneApp
import com.olavbg.javazone.ui.theme.JavaZoneTheme
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (!isGranted) {
            Log.w("MainActivity", "Notification permission denied. Reminders will not be shown.")
            showPermissionDeniedDialog()
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Varslinger er deaktivert")
            .setMessage("Uten tillatelse til å sende varslinger vil du ikke få påminnelser om foredragene du har lagt til som favoritter. Du kan endre dette i systeminnstillingene.")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        requestNotificationPermission()

        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "javazone.db")
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

        enableEdgeToEdge()
        setContent {
            JavaZoneTheme {
                JavaZoneApp(
                    repository,
                    settingsRepository,
                    reminderManager,
                    initialSessionId,
                    initialShowDonation
                )
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
