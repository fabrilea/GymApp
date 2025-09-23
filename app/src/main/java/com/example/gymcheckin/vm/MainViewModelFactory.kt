// MainViewModelFactory.kt
package com.example.gymcheckin.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gymcheckin.data.repository.ExcelRepository
import com.example.gymcheckin.data.ExcelSync
import com.example.gymcheckin.data.repository.GymRepository

class MainViewModelFactory(
    private val repo: GymRepository = ExcelRepository(ExcelSync()) // Excel-only
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
