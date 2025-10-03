package com.oxidelabmobile

import android.os.Bundle
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.oxidelabmobile.navigation.OxideLabNavHost
import com.oxidelabmobile.ui.theme.OxideLabMobileTheme

class MainActivity : ComponentActivity() {
    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            getSharedPreferences("app_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("first_run_permissions_requested", true)
                .apply()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        requestFirstRunPermissionsIfNeeded()

        setContent {
            OxideLabMobileTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    OxideLabNavHost(navController = navController)
                }
            }
        }
    }

    private fun requestFirstRunPermissionsIfNeeded() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val alreadyRequested = prefs.getBoolean("first_run_permissions_requested", false)
        if (alreadyRequested) return

        val required = buildRequiredPermissions()
        val toRequest = required.filter { perm ->
            ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED
        }

        if (toRequest.isEmpty()) {
            prefs.edit().putBoolean("first_run_permissions_requested", true).apply()
            return
        }

        requestPermissionsLauncher.launch(toRequest.toTypedArray())

        // Request special all-files access on Android 11+ by sending user to Settings page
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:" + packageName)
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        }
    }

    private fun buildRequiredPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            val perms = mutableListOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                perms += Manifest.permission.WRITE_EXTERNAL_STORAGE
            }
            perms
        }
    }
}