package com.github.se.eduverse.ui.converter

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Scanner
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.TopNavigationBar
import com.github.se.eduverse.viewmodel.PdfConverterViewModel

// Enum class to list the different options available in the PDF converter tool
enum class PdfConverterOption {
  TEXT_TO_PDF,
  IMAGE_TO_PDF,
  SCANNER_TOOL,
  SUMMARIZE_FILE,
  EXTRACT_TEXT,
  NONE
}

/**
 * Composable for the PDF converter tool screen
 *
 * @param navigationActions Actions to be performed for navigation
 * @param converterViewModel ViewModel for the PDF converter screen
 */
@Composable
fun PdfConverterScreen(
    navigationActions: NavigationActions,
    converterViewModel: PdfConverterViewModel = viewModel()
) {
  val context = LocalContext.current
  var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
  var showNameInputDialog by remember { mutableStateOf(false) }
  val pdfFileName = converterViewModel.newFileName.collectAsState()
  var currentPdfConverterOption by remember { mutableStateOf(PdfConverterOption.NONE) }

  // Launcher for opening the android file picker launcher
  val filePickerLauncher =
      rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { selectedFileUri = it }
        if (selectedFileUri != null) {
          val defaultFileName = getFileNameFromUri(context, selectedFileUri!!)
          converterViewModel.setNewFileName(defaultFileName)
          showNameInputDialog = true
        } else {
          Toast.makeText(context, "No file selected", Toast.LENGTH_LONG).show()
        }
      }

  Scaffold(
      topBar = {
        TopNavigationBar(screenTitle = "PDF Converter", navigationActions = navigationActions)
      }) { pd ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pd),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceEvenly) {
                    OptionCard(
                        testTag = PdfConverterOption.TEXT_TO_PDF.name,
                        optionName = "Text to PDF",
                        icon = Icons.AutoMirrored.Filled.TextSnippet,
                        onClick = {
                          currentPdfConverterOption = PdfConverterOption.TEXT_TO_PDF
                          filePickerLauncher.launch(
                              arrayOf(
                                  "text/plain",
                                  "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                        })
                    OptionCard(
                        testTag = PdfConverterOption.IMAGE_TO_PDF.name,
                        optionName = "Image to PDF",
                        icon = Icons.Default.Image,
                        onClick = {
                          currentPdfConverterOption = PdfConverterOption.IMAGE_TO_PDF
                          filePickerLauncher.launch(arrayOf("image/*"))
                        })
                  }

              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceEvenly) {
                    OptionCard(
                        testTag = PdfConverterOption.SCANNER_TOOL.name,
                        optionName = "Scanner tool",
                        icon = Icons.Default.Scanner,
                        onClick = {
                          currentPdfConverterOption = PdfConverterOption.SCANNER_TOOL
                          Toast.makeText(context, "Not available yet", Toast.LENGTH_LONG).show()
                        })
                    OptionCard(
                        testTag = PdfConverterOption.SUMMARIZE_FILE.name,
                        optionName = "Summarize file",
                        icon = Icons.Default.Summarize,
                        onClick = {
                          currentPdfConverterOption = PdfConverterOption.SUMMARIZE_FILE
                          Toast.makeText(context, "Not available yet", Toast.LENGTH_LONG).show()
                        })
                  }

              OptionCard(
                  testTag = PdfConverterOption.EXTRACT_TEXT.name,
                  optionName = "Extract text",
                  icon = Icons.Default.Abc,
                  onClick = {
                    currentPdfConverterOption = PdfConverterOption.EXTRACT_TEXT
                    Toast.makeText(context, "Not available yet", Toast.LENGTH_LONG).show()
                  })
            }
      }

  // Ask for the name of the PDF file to be created when the file to be converted is selected, and
  // launch the file convertion once the name is selected
  if (showNameInputDialog) {
    PdfNameInputDialog(
        pdfFileName = pdfFileName.value,
        onDismiss = {
          showNameInputDialog = false
          selectedFileUri = null
          Toast.makeText(context, "PDF creation canceled", Toast.LENGTH_SHORT).show()
        }, // On dismiss the pdf file creation is aborted
        onConfirm = { name ->
          converterViewModel.setNewFileName(name)
          showNameInputDialog = false
          when (currentPdfConverterOption) { // For each option, the corresponding actions are
            // performed
            PdfConverterOption.TEXT_TO_PDF -> {
              converterViewModel.convertDocumentToPdf(selectedFileUri, context)
            }
            PdfConverterOption.IMAGE_TO_PDF -> {
              converterViewModel.convertImageToPdf(selectedFileUri, context)
            }
            else -> {}
          }
          currentPdfConverterOption = PdfConverterOption.NONE // Reset the selected option
          selectedFileUri = null // Reset the selected file URI
        })
  }
}

/**
 * Composable for displaying a dialog to input the name of the PDF file to be created
 *
 * @param pdfFileName Default name of the PDF file
 * @param onDismiss Action to be performed when the dialog is dismissed
 * @param onConfirm Action to be performed when the dialog is confirmed
 */
@Composable
fun PdfNameInputDialog(pdfFileName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
  var inputText by remember { mutableStateOf(pdfFileName) } // Initialized with the default name

  AlertDialog(
      modifier = Modifier.testTag("pdfNameInputDialog"),
      onDismissRequest = onDismiss,
      title = { Text("Enter the name of the PDF file") },
      text = {
        OutlinedTextField(
            modifier = Modifier.testTag("pdfNameInput"),
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("PDF File Name") })
      },
      confirmButton = {
        Button(
            modifier = Modifier.testTag("confirmCreatePdfButton"),
            enabled = inputText.isNotEmpty(), // If input text is empty confirm button is disabled
            onClick = { onConfirm(inputText) }) {
              Text("Create PDF")
            }
      },
      dismissButton = {
        Button(onClick = onDismiss, modifier = Modifier.testTag("dismissCreatePdfButton")) {
          Text("Cancel")
        }
      })
}

/**
 * Composable for displaying the options of possible actions that can be performed within the PDF
 * converter tool
 *
 * @param testTag Test tag for the option
 * @param optionName Name of the option
 * @param icon Icon for the option
 * @param onClick Action to be performed when the option is clicked
 */
@Composable
fun OptionCard(testTag: String, optionName: String, icon: ImageVector, onClick: () -> Unit) {
  Card(
      modifier = Modifier.padding(16.dp).size(150.dp).clickable(onClick = onClick).testTag(testTag),
      elevation = CardDefaults.cardElevation(8.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFFC2F5F0))) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
              Icon(imageVector = icon, contentDescription = null, Modifier.size(80.dp))
              Text(optionName)
            }
      }
}

/**
 * Helper function to retrieve the file name from a the uri of a file
 *
 * @param context Context of the application
 * @param uri URI of the file
 * @return Name of the file
 */
private fun getFileNameFromUri(context: Context, uri: Uri): String {
  var fileName = ""
  val cursor = context.contentResolver.query(uri, null, null, null, null)
  cursor?.use {
    if (it.moveToFirst()) {
      fileName = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
    }
  }
  return fileName.substringBeforeLast(".") // Return the name without the file extension
}
