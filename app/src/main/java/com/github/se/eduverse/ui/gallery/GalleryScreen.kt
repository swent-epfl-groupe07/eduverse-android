package com.github.se.eduverse.ui.gallery

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.* // Import des layout
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.* // Material 3 API
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.github.se.eduverse.model.Photo
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.showBottomMenu
import com.github.se.eduverse.viewmodel.FolderViewModel
import com.github.se.eduverse.viewmodel.PhotoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    ownerId: String,
    photoViewModel: PhotoViewModel,
    folderViewModel: FolderViewModel,
    navigationActions: NavigationActions // Paramètre pour les actions de navigation
) {
  val context = LocalContext.current
  val photos by photoViewModel.photos.collectAsState()
  var selectedPhoto by remember { mutableStateOf<Photo?>(null) } // État pour l'image sélectionnée

  LaunchedEffect(ownerId) {
    photoViewModel.getPhotosByOwner(ownerId) // Récupère les photos pour cet utilisateur
  }

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text("My Gallery") },
            navigationIcon = {
              IconButton(
                  onClick = { navigationActions.goBack() },
                  modifier =
                      Modifier.testTag("GoBackButton") // Ajout du testTag pour le bouton de retour
                  ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Go back")
                  }
            })
      }) { contentPadding ->
        if (photos.isEmpty()) {
          Text(
              text = "No photos available.",
              modifier =
                  Modifier.fillMaxSize()
                      .padding(contentPadding)
                      .testTag(
                          "NoPhotosText"), // Ajout du testTag pour le texte "No photos available"
              textAlign = TextAlign.Center)
        } else {
          LazyVerticalGrid(
              columns = GridCells.Fixed(3),
              modifier = Modifier.fillMaxSize().padding(contentPadding),
              contentPadding = PaddingValues(16.dp)) {
                items(photos) { photo ->
                  val tag = "PhotoItem_${photo.path ?: "unknown"}"
                  PhotoItem(
                      photo, Modifier.testTag(tag)) { // Ajout du testTag pour chaque item photo
                        selectedPhoto = photo
                      }
                }
              }
        }
      }

  // Affiche le pop-up si une image est sélectionnée
  selectedPhoto?.let { photo ->
    ImageDialog(
        photo = photo,
        onDismiss = { selectedPhoto = null },
        onDownload = {
          // Appelle la fonction pour télécharger l'image
          downloadImage(photo.path ?: "unknown")
        },
        onAdd = {
          showBottomMenu(context, folderViewModel) { folder ->
            photoViewModel.makeFileFromPhoto(photo) {
              folderViewModel.createFileInFolder(it, it, folder)
            }
          }
        })
  }
}

@Composable
fun PhotoItem(photo: Photo, modifier: Modifier = Modifier, onClick: () -> Unit) {
  Image(
      painter = rememberAsyncImagePainter(photo.path),
      contentDescription = null,
      modifier =
          modifier.size(128.dp).padding(4.dp).clickable { onClick() } // Rend l'image cliquable
      )
}

@Composable
fun ImageDialog(photo: Photo, onDismiss: () -> Unit, onDownload: () -> Unit, onAdd: () -> Unit) {
  AlertDialog(
      onDismissRequest = { onDismiss() },
      text = {
        Box(
            modifier =
                Modifier.fillMaxWidth(0.8f) // Remplit 80% de la largeur de l'écran
                    .fillMaxHeight(0.8f) // Remplit 80% de la hauteur de l'écran
                    .testTag("ImageDialogBox") // Ajout du testTag pour le pop-up
            ) {
              Image(
                  painter = rememberAsyncImagePainter(photo.path),
                  contentDescription = null,
                  modifier = Modifier.fillMaxSize() // Remplit la boîte définie
                  )
            }
      },
      confirmButton = {
        TextButton(
            onClick = { onDismiss() },
            modifier = Modifier.testTag("CloseButton") // Ajout du testTag pour le bouton Close
            ) {
              Text("Close")
            }
      },
      dismissButton = {
        Row {
          IconButton(
              onClick = { onDownload() },
              modifier =
                  Modifier.testTag("DownloadButton") // Ajout du testTag pour le bouton Download
              ) {
                Icon(imageVector = Icons.Default.Download, contentDescription = "Download Image")
              }
          TextButton(onClick = { onAdd() }, modifier = Modifier.testTag("addToFolder")) {
            Text("Add to folder")
          }
        }
      })
}

// Fonction pour télécharger l'image dans la galerie
fun downloadImage(imagePath: String) {
  // Ici, tu peux implémenter le code pour télécharger l'image et l'enregistrer dans la galerie
  // Cela pourrait impliquer d'utiliser une bibliothèque ou API pour manipuler des fichiers et
  // stocker des images localement
  Log.d("GalleryScreen", "Downloading image from: $imagePath")
}

@Composable
@Preview
fun PreviewDialog() {
  ImageDialog(Photo("", byteArrayOf(0x01, 0x02)), {}, {}, {})
}
