package com.github.se.eduverse.ui.converter

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.se.eduverse.api.SUPPORTED_CONVERSION_TYPES
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
  var showInfoWindow by remember { mutableStateOf(false) }
  var showSelectSourceDialog by remember { mutableStateOf(false) }
  var inputFileMIMEType by remember { mutableStateOf("") }

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
                        explanation = "Converts a .txt file to PDF",
                        icon = Icons.AutoMirrored.Filled.TextSnippet,
                        onClick = {
                          currentPdfConverterOption = PdfConverterOption.TEXT_TO_PDF
                          inputFileMIMEType = "text/plain"
                          showInfoWindow = true
                        },
                        optionEnabled =
                            pdfConversionState.value ==
                                PdfConverterViewModel.PdfGenerationState.Ready)
                    OptionCard(
                        testTag = PdfConverterOption.IMAGE_TO_PDF.name,
                        optionName = "Image to PDF",
                        explanation = "Converts an image to PDF",
                        icon = Icons.Default.Image,
                        onClick = {
                          currentPdfConverterOption = PdfConverterOption.IMAGE_TO_PDF
                          inputFileMIMEType = "image/*"
                          showInfoWindow = true
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
                        explanation = "Converts a document to PDF",
                        icon = Icons.Default.PictureAsPdf,
                        onClick = {
                          currentPdfConverterOption = PdfConverterOption.DOCUMENT_TO_PDF
                          inputFileMIMEType = "*/*"
                          showInfoWindow = true
                        },
                        optionEnabled =
                            pdfConversionState.value ==
                                PdfConverterViewModel.PdfGenerationState.Ready)
                    OptionCard(
                        testTag = PdfConverterOption.SUMMARIZE_FILE.name,
                        optionName = "Summarize file",
                        explanation = "Generates a summary of a file",
                        icon = Icons.Default.Summarize,
                        onClick = {
                          currentPdfConverterOption = PdfConverterOption.SUMMARIZE_FILE
                          inputFileMIMEType = "application/pdf"
                          showInfoWindow = true
                        },
                        optionEnabled =
                            pdfConversionState.value ==
                                PdfConverterViewModel.PdfGenerationState.Ready)
                  }

              OptionCard(
                  testTag = PdfConverterOption.EXTRACT_TEXT.name,
                  optionName = "Extract text",
                  explanation = "Extracts text from an image",
                  icon = Icons.Default.Abc,
                  onClick = {
                    currentPdfConverterOption = PdfConverterOption.EXTRACT_TEXT
                    inputFileMIMEType = "image/*"
                    showInfoWindow = true
                  },
                  optionEnabled =
                      pdfConversionState.value == PdfConverterViewModel.PdfGenerationState.Ready)
            }
      }

  // Show the info window when an option is selected to inform the user what kind of file he needs
  // to select depending on the selected option
  if (showInfoWindow) {
    var title = ""
    var text = ""
    // Set the title and text of the info window depending on the selected option
    when (currentPdfConverterOption) {
      PdfConverterOption.TEXT_TO_PDF -> {
        title = "Text to PDF converter"
        text = "Select a .txt file to convert to PDF"
      }
      PdfConverterOption.IMAGE_TO_PDF -> {
        title = "Image to PDF converter"
        text = "Select an image to convert to PDF"
      }
      PdfConverterOption.DOCUMENT_TO_PDF -> {
        title = "Document to PDF converter"
        text =
            "Select a document to convert to PDF. Supported document types are: ${
                    SUPPORTED_CONVERSION_TYPES.joinToString(
                        ", "
                    )
                }"
      }
      PdfConverterOption.SUMMARIZE_FILE -> {
        title = "Pdf file summarizer"
        text = "Select a PDF file to summarize. The summary will be generated in a PDF file"
      }
      PdfConverterOption.EXTRACT_TEXT -> {
        title = "Text extractor"
        text =
            "Select an image to extract text from. Make sure the selected image contains text. The extracted text will be generated in a PDF file"
      }
      PdfConverterOption.NONE -> {
        showInfoWindow = false
      }
    }
    // Show the info window
    InfoWindow(
        title = title,
        text = text,
        onDismiss = { showInfoWindow = false },
        onConfirm = {
          showInfoWindow = false
          showSelectSourceDialog = true
        })
  }

  // Show a dialog that asks the user to choose from where to select the source file (device storage
  // or app folders) after the info window has been displayed
  if (showSelectSourceDialog) {
    SelectSourceFileDialog(
        onDismiss = { showSelectSourceDialog = false },
        onDeviceStorageClick = {
          showSelectSourceDialog = false
          filePickerLauncher.launch(arrayOf(inputFileMIMEType))
        },
        onFoldersClick = {
          showSelectSourceDialog = false
          /** logic for this option will be added later */
        })
  }

  // Ask for the name of the PDF file to be created when the input file has been selected, and
  // launch the pdf file generation on confirm
  if (showNameInputDialog) {
    PdfNameInputDialog(
        pdfFileName = pdfFileName.value,
        onDismiss = {
          showNameInputDialog = false
          context.showToast("PDF file generation cancelled")
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
      title = {
        Text(
            "Enter a name for the PDF file that will be generated",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center)
      },
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
 * @param explanation Brief explanation on what the option does
 * @param icon Icon for the option
 * @param onClick Action to be performed when the option is clicked
 */
@Composable
fun OptionCard(
    testTag: String,
    optionName: String,
    explanation: String,
    icon: ImageVector,
    onClick: () -> Unit,
    optionEnabled: Boolean
) {
  Card(
      modifier =
          Modifier.padding(8.dp)
              .size(150.dp)
              .clickable(onClick = onClick, enabled = optionEnabled)
              .testTag(testTag),
      colors =
          CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
              Icon(imageVector = icon, contentDescription = null, Modifier.size(80.dp))
              Text(optionName, fontSize = 16.sp, fontWeight = FontWeight.Bold)
              Text(explanation, fontSize = 8.sp)
            }
      }
}

/**
 * Composable for displaying a loading indicator while the PDF is being generated
 *
 * @param onAbort Action to be performed when the abort PDF generation button is clicked
 */
@Composable
fun LoadingIndicator(onAbort: () -> Unit) {
  Column(
      modifier = Modifier.fillMaxSize().testTag("loadingIndicator"),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(
            color = Color.LightGray,
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
 * Composable for displaying an info window which informs the user what type of file the selected
 * option accepts as input (and when necessary that the output is going to be in a pdf file) and
 * invites him to select an input or cancel the pdf generation
 *
 * @param title Title of the info window
 * @param text Text body of the info window
 * @param onDismiss Action to be performed when the dialog is dismissed
 * @param onConfirm Action to be performed when the dialog is confirmed
 */
@Composable
fun InfoWindow(title: String, text: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
  AlertDialog(
      modifier = Modifier.testTag("infoWindow"),
      onDismissRequest = onDismiss,
      title = {
        Text(
            title,
            modifier = Modifier.fillMaxWidth().testTag("infoWindowTitle"),
            textAlign = TextAlign.Center)
      },
      text = { Text(text, modifier = Modifier.fillMaxWidth().testTag("infoWindowText")) },
      confirmButton = {
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().testTag("infoWindowConfirmButton")) {
              Text("Select file")
            }
      },
      dismissButton = {
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().testTag("infoWindowDismissButton")) {
              Text("Cancel")
            }
      })
}

/**
 * Composable for displaying a dialog for the user to choose if he wants to select the input file
 * from local device's files or from the app's folders
 *
 * @param onDismiss Action to be performed when the dialog is dismissed
 * @param onDeviceStorageClick Action to be performed when the device storage button is clicked
 * @param onFoldersClick Action to be performed when the app folders button is clicked
 */
@Composable
fun SelectSourceFileDialog(
    onDismiss: () -> Unit,
    onDeviceStorageClick: () -> Unit,
    onFoldersClick: () -> Unit
) {
  AlertDialog(
      modifier = Modifier.testTag("selectSourceFileDialog"),
      onDismissRequest = onDismiss,
      title = {
        Text(
            text = "Choose from where to select the source file",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center)
      },
      text = {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally) {
              // Button to proceed towards selecting a file from the app folders
              Button(
                  onClick = { onFoldersClick() },
                  modifier = Modifier.fillMaxWidth().testTag("appFoldersButton")) {
                    Text("App folders")
                  }
              // Button to launch the file picker
              Button(
                  onClick = { onDeviceStorageClick() },
                  modifier = Modifier.fillMaxWidth().testTag("deviceStorageButton")) {
                    Text("Device storage")
                  }
            }
      },
      confirmButton = {},
      dismissButton = {
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().testTag("selectSourceFileDismissButton")) {
              Text("Cancel")
            }
      })
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
