package com.github.se.eduverse.viewmodel

#generate the imports
import androidx.lifecycle.ViewModel
import com.github.se.eduverse.model.Folder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

open class PdfConverterViewModel(){
    private val _selectedFolder = MutableStateFlow<Folder?>(null)
    val selectedFolder: StateFlow<Folder?> = _selectedFolder.asStateFlow()

}