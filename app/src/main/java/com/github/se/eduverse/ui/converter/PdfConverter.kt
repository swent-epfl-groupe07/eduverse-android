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
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.se.eduverse.showToast
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.TopNavigationBar
import com.github.se.eduverse.viewmodel.PdfConverterViewModel

// Enum class to list the different options available in the PDF converter tool
enum class PdfConverterOption {
  TEXT_TO_PDF,
  IMAGE_TO_PDF,
  DOCUMENT_TO_PDF,
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
    converterViewModel: PdfConverterViewModel = viewModel(factory = PdfConverterViewModel.Factory)
) {
  val context = LocalContext.current
  var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
  var showNameInputDialog by remember { mutableStateOf(false) }
  val pdfFileName = converterViewModel.newFileName.collectAsState()
  var currentPdfConverterOption by remember { mutableStateOf(PdfConverterOption.NONE) }
  val pdfConversionState = converterViewModel.pdfGenerationState.collectAsState()

  // Launcher for opening the android file picker launcher
  val filePickerLauncher =
      rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
          selectedFileUri = uri
          val defaultFileName = getFileNameFromUri(context, selectedFileUri!!)
          converterViewModel.setNewFileName(defaultFileName)
          showNameInputDialog = true
        } else {
          context.showToast("No file selected")
        }
      }

  Scaffold(
      topBar = {
        TopNavigationBar(screenTitle = "PDF Converter", navigationActions = navigationActions)
      },
      bottomBar = {
        BottomNavigationMenu({ navigationActions.navigateTo(it) }, LIST_TOP_LEVEL_DESTINATION, "")
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
                          filePickerLauncher.launch(arrayOf("text/plain"))
                        },
                        optionEnabled =
                            pdfConversionState.value ==
                                PdfConverterViewModel.PdfGenerationState.Ready)
                    OptionCard(
                        testTag = PdfConverterOption.IMAGE_TO_PDF.name,
                        optionName = "Image to PDF",
                        icon = Icons.Default.Image,
                        onClick = {
                          currentPdfConverterOption = PdfConverterOption.IMAGE_TO_PDF
                          filePickerLauncher.launch(arrayOf("image/*"))
                        },
                        optionEnabled =
                            pdfConversionState.value ==
                                PdfConverterViewModel.PdfGenerationState.Ready)
                  }

              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceEvenly) {
                    OptionCard(
                        testTag = PdfConverterOption.DOCUMENT_TO_PDF.name,
                        optionName = "Doc to PDF",
                        icon = Icons.Default.PictureAsPdf,
                        onClick = {
                          currentPdfConverterOption = PdfConverterOption.DOCUMENT_TO_PDF
                          Toast.makeText(context, "Not available yet", Toast.LENGTH_LONG).show()
                        },
                        optionEnabled =
                            pdfConversionState.value ==
                                PdfConverterViewModel.PdfGenerationState.Ready)
                    OptionCard(
                        testTag = PdfConverterOption.SUMMARIZE_FILE.name,
                        optionName = "Summarize file",
                        icon = Icons.Default.Summarize,
                        onClick = {
                          currentPdfConverterOption = PdfConverterOption.SUMMARIZE_FILE
                          Toast.makeText(context, "Not available yet", Toast.LENGTH_LONG).show()
                        },
                        optionEnabled =
                            pdfConversionState.value ==
                                PdfConverterViewModel.PdfGenerationState.Ready)
                  }

              OptionCard(
                  testTag = PdfConverterOption.EXTRACT_TEXT.name,
                  optionName = "Extract text",
                  icon = Icons.Default.Abc,
                  onClick = {
                    currentPdfConverterOption = PdfConverterOption.EXTRACT_TEXT
                    Toast.makeText(context, "Not available yet", Toast.LENGTH_LONG).show()
                  },
                  optionEnabled =
                      pdfConversionState.value == PdfConverterViewModel.PdfGenerationState.Ready)
            }
      }

  // Ask for the name of the PDF file to be created when the file to be converted is selected, and
  // launch the file convertion once the name is selected
  if (showNameInputDialog) {
    PdfNameInputDialog(
        pdfFileName = pdfFileName.value,
        onDismiss = {
          showNameInputDialog = false
          context.showToast("PDF file creation cancelled")
        }, // On dismiss the pdf file creation is not launched
        onConfirm = { name ->
          converterViewModel.setNewFileName(name)
          showNameInputDialog = false
          converterViewModel.generatePdf(selectedFileUri!!, context, currentPdfConverterOption)
        })
  }

  // Handle the different states of the PDF generation process
  when (val conversionState = pdfConversionState.value) {
    is PdfConverterViewModel.PdfGenerationState.InProgress -> {
      LoadingIndicator { converterViewModel.abortPdfGeneration() }
    }
    is PdfConverterViewModel.PdfGenerationState.Aborted -> {
      context.showToast("PDF generation aborted")
      converterViewModel.setPdfGenerationStateToReady()
    }
    is PdfConverterViewModel.PdfGenerationState.Error -> {
      context.showToast("Failed to generate PDF")
      converterViewModel.setPdfGenerationStateToReady()
    }
    is PdfConverterViewModel.PdfGenerationState.Ready -> {}
    is PdfConverterViewModel.PdfGenerationState.Success -> {
      context.showToast("Pdf created successfully")
      converterViewModel.savePdfToDevice(conversionState.pdfFile, context)
      converterViewModel.setPdfGenerationStateToReady()
    }
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
fun OptionCard(
    testTag: String,
    optionName: String,
    icon: ImageVector,
    onClick: () -> Unit,
    optionEnabled: Boolean
) {
  Card(
      modifier =
          Modifier.padding(16.dp)
              .size(150.dp)
              .clickable(onClick = onClick, enabled = optionEnabled)
              .testTag(testTag),
      elevation = CardDefaults.cardElevation(8.dp),
      colors =
          CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.tertiary,
              contentColor = MaterialTheme.colorScheme.onTertiary)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
              Icon(imageVector = icon, contentDescription = null, Modifier.size(80.dp))
              Text(optionName)
            }
      }
}

@Composable
fun LoadingIndicator(onAbort: () -> Unit) {
  Column(
      modifier = Modifier.fillMaxSize().testTag("loadingIndicator"),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(
            color = Color.Gray,
            trackColor = Color.Cyan,
            strokeWidth = 10.dp,
            strokeCap = StrokeCap.Round)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onAbort, modifier = Modifier.testTag("abortButton")) {
          Text("Abort PDF Generation")
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
