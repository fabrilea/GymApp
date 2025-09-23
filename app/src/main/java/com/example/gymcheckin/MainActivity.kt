// MainActivity.kt
package com.example.gymcheckin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import com.example.gymcheckin.data.ExcelLinkStore
import com.example.gymcheckin.ui.GymApp
import com.example.gymcheckin.vm.MainViewModel
import com.example.gymcheckin.vm.MainViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val factory = MainViewModelFactory()

        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                val vm: MainViewModel =
                    androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
                GymApp(vm)
            }
        }

        // File picker para vincular Excel
        val launcher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                    // Algunos providers no soportan persistente; lo ignoramos
                }
                ExcelLinkStore.setUri(this, uri)
                Toast.makeText(this, "Excel vinculado", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
