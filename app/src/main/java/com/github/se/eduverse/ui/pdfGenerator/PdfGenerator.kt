package com.github.se.eduverse.ui.pdfGenerator

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.se.eduverse.api.SUPPORTED_CONVERSION_TYPES
import com.github.se.eduverse.isNetworkAvailable
import com.github.se.eduverse.model.Folder
import com.github.se.eduverse.showToast
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.TopNavigationBar
import com.github.se.eduverse.ui.speechRecognition.SpeechRecognizerInterface
import com.github.se.eduverse.viewmodel.FolderViewModel
import com.github.se.eduverse.viewmodel.PdfGeneratorViewModel
import java.io.File

// Enum class to list the different options available in the PDF converter tool
enum class PdfGeneratorOption {
  TEXT_TO_PDF,
  IMAGE_TO_PDF,
  DOCUMENT_TO_PDF,
  SUMMARIZE_FILE,
  EXTRACT_TEXT,
  TRANSCRIBE_SPEECH,
  NONE
}

/**
 * Composable for the PDF converter tool screen
 *
 * @param navigationActions Actions to be performed for navigation
 * @param converterViewModel ViewModel for the PDF converter screen
 */
@Composable
fun PdfGeneratorScreen(
    navigationActions: NavigationActions,
    converterViewModel: PdfGeneratorViewModel = viewModel(factory = PdfGeneratorViewModel.Factory),
    folderViewModel: FolderViewModel = viewModel(factory = FolderViewModel.Factory),
    context: Context = LocalContext.current
) {
  var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
  var showNameInputDialog by remember { mutableStateOf(false) }
  val pdfFileName = converterViewModel.newFileName.collectAsState()
  var currentPdfGeneratorOption by remember { mutableStateOf(PdfGeneratorOption.NONE) }
  val pdfConversionState = converterViewModel.pdfGenerationState.collectAsState()
  var showInfoWindow by remember { mutableStateOf(false) }
  var inputFileMIMEType by remember { mutableStateOf("") }
  var showDestinationDialog by remember { mutableStateOf(false) }
  var showSelectFolderDialog by remember { mutableStateOf(false) }
  val folders by folderViewModel.folders.collectAsState()
  var showInputNewFolderNameDialog by remember { mutableStateOf(false) }
  var generatedPdf by remember { mutableStateOf<File?>(null) }
  var showTranscriptionDialog by remember { mutableStateOf(false) }

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
      topBar = { TopNavigationBar(navigationActions = navigationActions, screenTitle = null) },
      bottomBar = {
        BottomNavigationMenu({ navigationActions.navigateTo(it) }, LIST_TOP_LEVEL_DESTINATION, "")
      }) { pd ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pd),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally) {
              item {
                OptionCard(
                    testTag = PdfGeneratorOption.TEXT_TO_PDF.name,
                    optionName = "Text to PDF",
                    explanation = "Converts a .txt file to PDF",
                    icon = Icons.AutoMirrored.Filled.TextSnippet,
                    onClick = {
                      currentPdfGeneratorOption = PdfGeneratorOption.TEXT_TO_PDF
                      inputFileMIMEType = "text/plain"
                      showInfoWindow = true
                    },
                    optionEnabled =
                        pdfConversionState.value == PdfGeneratorViewModel.PdfGenerationState.Ready)
              }
              item {
                OptionCard(
                    testTag = PdfGeneratorOption.IMAGE_TO_PDF.name,
                    optionName = "Image to PDF",
                    explanation = "Converts an image to PDF",
                    icon = Icons.Default.Image,
                    onClick = {
                      currentPdfGeneratorOption = PdfGeneratorOption.IMAGE_TO_PDF
                      inputFileMIMEType = "image/*"
                      showInfoWindow = true
                    },
                    optionEnabled =
                        pdfConversionState.value == PdfGeneratorViewModel.PdfGenerationState.Ready)
              }
              item {
                OptionCard(
                    testTag = PdfGeneratorOption.DOCUMENT_TO_PDF.name,
                    optionName = "Doc to PDF",
                    explanation = "Converts a document to PDF",
                    icon = Icons.Default.PictureAsPdf,
                    onClick = {
                      if (!context.isNetworkAvailable()) {
                        showOfflineToast(context)
                      } else {
                        currentPdfGeneratorOption = PdfGeneratorOption.DOCUMENT_TO_PDF
                        inputFileMIMEType = "*/*"
                        showInfoWindow = true
                      }
                    },
                    optionEnabled =
                        pdfConversionState.value == PdfGeneratorViewModel.PdfGenerationState.Ready)
              }
              item {
                OptionCard(
                    testTag = PdfGeneratorOption.SUMMARIZE_FILE.name,
                    optionName = "Summarize file",
                    explanation = "Generates a summary of a file",
                    icon = Icons.Default.Summarize,
                    onClick = {
                      if (!context.isNetworkAvailable()) {
                        showOfflineToast(context)
                      } else {
                        currentPdfGeneratorOption = PdfGeneratorOption.SUMMARIZE_FILE
                        inputFileMIMEType = "application/pdf"
                        showInfoWindow = true
                      }
                    },
                    optionEnabled =
                        pdfConversionState.value == PdfGeneratorViewModel.PdfGenerationState.Ready)
              }
              item {
                OptionCard(
                    testTag = PdfGeneratorOption.EXTRACT_TEXT.name,
                    optionName = "Extract text",
                    explanation = "Extracts text from an image",
                    icon = Icons.Default.Abc,
                    onClick = {
                      currentPdfGeneratorOption = PdfGeneratorOption.EXTRACT_TEXT
                      inputFileMIMEType = "image/*"
                      showInfoWindow = true
                    },
                    optionEnabled =
                        pdfConversionState.value == PdfGeneratorViewModel.PdfGenerationState.Ready)
              }
              item {
                OptionCard(
                    testTag = PdfGeneratorOption.TRANSCRIBE_SPEECH.name,
                    optionName = "Speech to PDF",
                    explanation = "Transcribes speech to text",
                    icon = Icons.Default.Mic,
                    onClick = {
                      if (SpeechRecognizer.isRecognitionAvailable(context)) {
                        currentPdfGeneratorOption = PdfGeneratorOption.TRANSCRIBE_SPEECH
                        showTranscriptionDialog = true
                      } else {
                        // Show toast if speech recognition is not provided by the device
                        context.showToast(
                            "Cannot use this tool. Speech recognition not supported by your device.")
                      }
                    },
                    optionEnabled =
                        pdfConversionState.value == PdfGeneratorViewModel.PdfGenerationState.Ready)
              }
            }
      }

  // Show the info window when an option is selected to inform the user what kind of file he needs
  // to select depending on the selected option
  if (showInfoWindow) {
    var title = ""
    var text = ""
    // Set the title and text of the info window depending on the selected option
    when (currentPdfGeneratorOption) {
      PdfGeneratorOption.TEXT_TO_PDF -> {
        title = "Text to PDF converter"
        text = "Select a .txt file to convert to PDF"
      }
      PdfGeneratorOption.IMAGE_TO_PDF -> {
        title = "Image to PDF converter"
        text = "Select an image to convert to PDF"
      }
      PdfGeneratorOption.DOCUMENT_TO_PDF -> {
        title = "Document to PDF converter"
        text =
            "Select a document to convert to PDF. Supported document types are: ${
                        SUPPORTED_CONVERSION_TYPES.joinToString(
                            ", "
                        )
                    }"
      }
      PdfGeneratorOption.SUMMARIZE_FILE -> {
        title = "Pdf file summarizer"
        text = "Select a PDF file to summarize. The summary will be generated in a PDF file"
      }
      PdfGeneratorOption.EXTRACT_TEXT -> {
        title = "Text extractor"
        text =
            "Select an image to extract text from. Make sure the selected image contains text. The extracted text will be generated in a PDF file"
      }
      else -> {
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
          filePickerLauncher.launch(arrayOf(inputFileMIMEType))
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
          converterViewModel.generatePdf(selectedFileUri!!, context, currentPdfGeneratorOption)
        })
  }

  // Handle the different states of the PDF generation process
  when (val conversionState = pdfConversionState.value) {
    is PdfGeneratorViewModel.PdfGenerationState.InProgress -> {
      LoadingIndicator { converterViewModel.abortPdfGeneration() }
    }
    is PdfGeneratorViewModel.PdfGenerationState.Aborted -> {
      context.showToast("PDF generation aborted")
      converterViewModel.setPdfGenerationStateToReady()
    }
    is PdfGeneratorViewModel.PdfGenerationState.Error -> {
      context.showToast("Error: ${conversionState.message}")
      converterViewModel.setPdfGenerationStateToReady()
    }
    is PdfGeneratorViewModel.PdfGenerationState.Ready -> {}
    is PdfGeneratorViewModel.PdfGenerationState.Success -> {
      generatedPdf =
          (pdfConversionState.value as PdfGeneratorViewModel.PdfGenerationState.Success).pdfFile
      converterViewModel.setPdfGenerationStateToReady()
      showDestinationDialog = true
    }
  }

  // Ask the user to choose where to save the generated PDF file
  if (showDestinationDialog) {
    SelectDestinationDialog(
        onDiscard = {
          showDestinationDialog = false
          converterViewModel.deleteGeneratedPdf()
        },
        onDeviceStorageClick = {
          showDestinationDialog = false
          converterViewModel.savePdfToDevice(generatedPdf!!, context)
        },
        onFoldersClick = {
          if (!context.isNetworkAvailable()) {
            // If the device is offline the file can't be uploaded to firebase storage
            context.showToast(
                "Your device is offline. Please connect to the internet to be able to save to folders")
          } else {
            showDestinationDialog = false
            showSelectFolderDialog = true
          }
        })
  }

  // Show the dialog to select the destination folder from the app's folders
  if (showSelectFolderDialog) {
    SelectFolderDialog(
        folders = folders,
        onDismiss = {
          showSelectFolderDialog = false
          showDestinationDialog = true
        },
        onSelect = { folder ->
          showSelectFolderDialog = false
          converterViewModel.savePdfToFolder(
              folder,
              Uri.fromFile(generatedPdf),
              context,
              { converterViewModel.deleteGeneratedPdf() },
              {
                showDestinationDialog =
                    true // Show the destination dialog to allow the user to still save the file to
                // local storage if the upload fails
              })
        },
        onCreate = {
          showSelectFolderDialog = false
          showInputNewFolderNameDialog = true
        })
  }

  // Show the dialog to input the name of the new folder
  if (showInputNewFolderNameDialog) {
    InputNewFolderNameDialog(
        onDismiss = {
          showInputNewFolderNameDialog = false
          showSelectFolderDialog = true
        },
        onConfirm = { name ->
          showInputNewFolderNameDialog = false
          folderViewModel.createNewFolderFromName(
              name,
              {
                converterViewModel.savePdfToFolder(
                    it,
                    Uri.fromFile(generatedPdf),
                    context,
                    { converterViewModel.deleteGeneratedPdf() },
                    {
                      showDestinationDialog =
                          true // Show the destination dialog to allow the user to still save the
                      // file to local storage if the upload fails
                    })
              },
              {
                context.showToast(it)
                // Show the destination dialog to allow the user to still save the file to local
                // storage if the folder creation fails
                showDestinationDialog = true
              })
        })
  }

  // Show the transcription dialog when the user selects the speech to text option
  if (showTranscriptionDialog) {
    var transcriptionFile: File? = null
    converterViewModel.createTranscriptionFile({ transcriptionFile = it }) {
      showTranscriptionDialog = false
      context.showToast("Error creating transcription file: $it")
    }
    LaunchTranscriptionDialog(
        context = context,
        textFile = transcriptionFile!!,
        onDismiss = {
          showTranscriptionDialog = false
          context.showToast("Speech to text transcription cancelled")
          converterViewModel.resetTranscriptionFile()
        },
        onFinish = {
          showTranscriptionDialog = false
          selectedFileUri = Uri.fromFile(transcriptionFile)
          showNameInputDialog = true
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
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .padding(8.dp)
              .height(110.dp)
              .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp), clip = false)
              .background(
                  Brush.horizontalGradient(
                      colors =
                          listOf(
                              MaterialTheme.colorScheme.secondary,
                              MaterialTheme.colorScheme.primary)),
                  shape = RoundedCornerShape(8.dp))
              .clickable(enabled = optionEnabled, onClick = onClick)
              .padding(horizontal = 16.dp, vertical = 12.dp)
              .testTag(testTag),
      verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (optionEnabled) MaterialTheme.colorScheme.onSecondary else Color.Gray,
            modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
          Text(
              text = optionName,
              style =
                  MaterialTheme.typography.bodyLarge.copy(
                      fontWeight = FontWeight.Bold,
                      color =
                          if (optionEnabled) MaterialTheme.colorScheme.onSecondary else Color.Gray))
          Text(
              text = explanation,
              style =
                  MaterialTheme.typography.bodySmall.copy(
                      color =
                          if (optionEnabled)
                              MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.7f)
                          else Color.Gray.copy(alpha = 0.7f)))
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
 * invites him to select an input file or cancel the pdf generation
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
 * Composable for displaying a dialog for the user to choose if he wants to save the generated PDF
 * file in local device's storage or in one of the app's folders or discard the generated PDF
 *
 * @param onDiscard Action to be performed when the discard PDF button is clicked
 * @param onDeviceStorageClick Action to be performed when the device storage button is clicked
 * @param onFoldersClick Action to be performed when the app folders button is clicked
 */
@Composable
fun SelectDestinationDialog(
    onDiscard: () -> Unit,
    onDeviceStorageClick: () -> Unit,
    onFoldersClick: () -> Unit
) {
  AlertDialog(
      modifier = Modifier.testTag("selectDestinationDialog"),
      onDismissRequest = {
        /**
         * Don't allow the user to dismiss the dialog by clicking outside of it, the user needs to
         * explicitly take a decision regarding the generated file to avoid losing the file by
         * mistake and having to restart the file generation.
         */
      },
      title = {
        Text(
            text = "PDF file generation is complete. Choose where to save it.",
            modifier = Modifier.fillMaxWidth().testTag("selectDestinationDialogTitle"),
            textAlign = TextAlign.Center)
      },
      text = {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally) {
              // Button to proceed towards selecting the folder in which the user wants to save the
              // PDF from the app folders
              Button(
                  onClick = { onFoldersClick() },
                  modifier = Modifier.fillMaxWidth().testTag("appFoldersButton")) {
                    Text("App folders")
                  }
              // Button to save the PDF in the device storage
              Button(
                  onClick = { onDeviceStorageClick() },
                  modifier = Modifier.fillMaxWidth().testTag("deviceStorageButton")) {
                    Text("Device storage")
                  }
            }
      },
      confirmButton = {},
      dismissButton = {
        Button(onClick = onDiscard, modifier = Modifier.fillMaxWidth().testTag("discardButton")) {
          Text("Discard PDF", color = Color.Red)
        }
      })
}

/**
 * Composable for displaying a dialog to select the destination folder from the app's folders
 *
 * @param folders List of folders to be displayed
 * @param onDismiss Action to be performed when the dialog is dismissed
 * @param onSelect Action to be performed when a folder is selected
 */
@Composable
fun SelectFolderDialog(
    folders: List<Folder>,
    onDismiss: () -> Unit,
    onSelect: (Folder) -> Unit,
    onCreate: () -> Unit
) {
  AlertDialog(
      onDismissRequest = { onDismiss() },
      title = {
        Text(
            "Select destination folder",
            modifier = Modifier.fillMaxWidth().testTag("selectFolderDialogTitle"),
            textAlign = TextAlign.Center)
      },
      text = {
        if (folders.isEmpty()) {
          Text(
              "No folders found. Create a new folder to save the generated PDF file.",
              modifier = Modifier.fillMaxWidth().testTag("noFoldersFoundText"),
              textAlign = TextAlign.Center)
        } else {
          LazyColumn {
            items(folders.size) { i ->
              val folder = folders[i]
              TextButton(
                  modifier = Modifier.testTag("folderButton_${folder.id}"),
                  onClick = { onSelect(folder) }) {
                    Text(folder.name)
                  }
              HorizontalDivider()
            }
          }
        }
      },
      confirmButton = {
        // Allow the user to create a new folder in which to store the generated pdf
        Button(
            modifier = Modifier.fillMaxWidth().testTag("createFolderButton"), onClick = onCreate) {
              Text("Create a new folder")
            }
      },
      dismissButton = {
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().testTag("cancelSelectFolderButton")) {
              Text("Cancel")
            }
      },
      modifier = Modifier.testTag("selectFolderDialog"))
}

/**
 * Composable for displaying a dialog to input the name of the new folder on folder creation
 *
 * @param onDismiss Action to be performed when the dialog is dismissed
 * @param onConfirm Action to be performed when the dialog is confirmed
 */
@Composable
fun InputNewFolderNameDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
  var inputText by remember { mutableStateOf("") }

  AlertDialog(
      modifier = Modifier.testTag("inputNewFolderNameDialog"),
      onDismissRequest = onDismiss,
      title = {
        Text(
            "Enter a name for the new folder",
            modifier = Modifier.fillMaxWidth().testTag("inputNewFolderNameDialogTitle"),
            textAlign = TextAlign.Center)
      },
      text = {
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Folder name") },
            modifier = Modifier.fillMaxWidth().testTag("inputNewFolderNameField"))
      },
      confirmButton = {
        Button(
            enabled = inputText.isNotEmpty(),
            onClick = { onConfirm(inputText) },
            modifier = Modifier.testTag("confirmCreateFolderButton")) {
              Text("Create folder")
            }
      },
      dismissButton = {
        Button(onClick = onDismiss, modifier = Modifier.testTag("dismissCreateFolderButton")) {
          Text("Cancel")
        }
      })
}

/**
 * Composable for displaying a dialog to transcribe speech to text
 *
 * @param context Context of the application
 * @param textFile Temporary file in which to store the transcribed text (used to generate the PDF
 *   file later)
 * @param onDismiss Action to be performed when the dialog is dismissed
 * @param onFinish Action to be performed when the dialog is confirmed
 */
@Composable
fun LaunchTranscriptionDialog(
    context: Context,
    textFile: File,
    onDismiss: () -> Unit,
    onFinish: () -> Unit
) {
  var canGeneratePdf by remember { mutableStateOf(false) }
  SpeechRecognizerInterface(
      context = context,
      title = "Speech to PDF transcriber",
      description =
          "Use the bottom button to capture your speech. Each successful recording is transcribed as a new paragraph into a PDF file. Record as many times as needed in order to add multiple paragraphs. Press 'Generate PDF' to finish the transcription and generate the PDF file. \n⚠\uFE0F Pressing 'Cancel' discards all transcribed text.",
      onDismiss = onDismiss,
      onResult = { text ->
        canGeneratePdf = true
        textFile.appendText("\n$text")
      }) { enabled ->
        Button(
            onClick = {
              if (canGeneratePdf) {
                onFinish()
              } else {
                context.showToast("Cannot generate PDF. No speech detected yet.")
              }
            },
            enabled = enabled,
            modifier = Modifier.testTag("transcriptionFinishButton")) {
              Text("Generate PDF")
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

/**
 * Helper function used to show a toast message when the device is offline and the tool requires an
 * internet connection
 *
 * @param context The context of the application
 */
private fun showOfflineToast(context: Context) {
  context.showToast("Your device is offline. Please connect to the internet to use this tool.")
}
