package com.github.se.eduverse.ui.camera

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.github.se.eduverse.R
import com.github.se.eduverse.model.Photo
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.PhotoViewModel
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.io.FileOutputStream

@Composable
fun NextScreen(photoFile: File?, navigationActions: NavigationActions, viewModel: PhotoViewModel) {
  val context = LocalContext.current
  val auth = FirebaseAuth.getInstance()
  val ownerId = auth.currentUser?.uid ?: "anonymous"
  val path = "photos/$ownerId/${System.currentTimeMillis()}.jpg"

  val bitmap = photoFile?.let { BitmapFactory.decodeFile(it.path)?.asImageBitmap() }

  Box(modifier = Modifier.fillMaxSize().background(Color(0xFFEBF1F4)).padding(16.dp)) {
    bitmap?.let {
      Column(
          modifier =
              Modifier.align(Alignment.TopStart)
                  .padding(top = 56.dp, start = 16.dp)
                  .background(color = Color(0xFFD9D9D9), shape = RoundedCornerShape(size = 10.dp))
                  .width(133.dp)
                  .height(164.dp)
                  .testTag("imagePreviewContainer"),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                bitmap = adjustImageRotation(it.asAndroidBitmap()).asImageBitmap(),
                contentDescription = "Preview",
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier.fillMaxSize()
                        .clip(RoundedCornerShape(size = 15.dp))
                        .background(color = Color(0xFFD9D9D9))
                        .testTag("previewImage"))

            /*Text(
                text = "Edit cover",
                color = Color.Gray,
                modifier = Modifier
                    .background(Color.White, shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("editCoverText")
            )*/
            // the button edit cover will be implemented later
          }
    }

    Text(
        text = "Add description...",
        color = Color.Gray,
        fontSize = 24.sp,
        modifier =
            Modifier.align(Alignment.TopStart)
                .padding(start = 16.dp, top = 240.dp)
                .testTag("addDescriptionText"),
        style =
            TextStyle(
                fontSize = 24.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0x424A4459),
                letterSpacing = 0.24.sp,
            ))

    Column(
        modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp, top = 320.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
          StyledButton(
              text = " Add link",
              iconRes = R.drawable.add,
              bitmap = bitmap,
              context = context,
              testTag = "addLinkButton")
          StyledButton(
              text = " More options",
              iconRes = R.drawable.more_horiz,
              bitmap = bitmap,
              context = context,
              testTag = "moreOptionsButton")
          StyledButton(
              text = " Share to",
              iconRes = R.drawable.share,
              bitmap = bitmap,
              context = context,
              testTag = "shareToButton")
        }

    Row(
        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          Button(
              onClick = {
                bitmap?.let { bmp ->
                  val byteArray = imageBitmapToByteArray(bmp)
                  val photo = Photo(ownerId, byteArray, path)
                  viewModel.savePhoto(photo)
                  navigationActions.goBack()
                  navigationActions.goBack()
                  navigationActions.goBack()
                }
              },
              modifier = Modifier.weight(1f).height(56.dp).testTag("saveButton"),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC6D3E1)),
              shape = RoundedCornerShape(8.dp)) {
                Text(
                    text = "Save",
                    style =
                        TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight(500),
                            color = Color(0xFF4A4459),
                            textAlign = TextAlign.Center),
                    modifier = Modifier.padding(horizontal = 8.dp))
              }

          Button(
              onClick = { /* Logique pour publier */},
              modifier = Modifier.weight(1f).height(56.dp).testTag("postButton"),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37CED5)),
              shape = RoundedCornerShape(8.dp)) {
                Text(
                    text = "Post",
                    style =
                        TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight(500),
                            textAlign = TextAlign.Center,
                            color = Color(0xFF4A4459),
                        ),
                    modifier = Modifier.width(50.dp).height(24.dp))
              }
        }

    Box(
        modifier =
            Modifier.align(Alignment.TopStart)
                .size(40.dp)
                .clickable { navigationActions.goBack() }
                .padding(4.dp)
                .testTag("closeButton")) {
          Image(
              painter = painterResource(id = R.drawable.close),
              contentDescription = "Close button",
              modifier = Modifier.fillMaxSize().padding(4.dp),
              contentScale = ContentScale.Fit)
        }
  }
}

@Composable
fun StyledButton(
    text: String,
    iconRes: Int,
    bitmap: ImageBitmap?,
    context: Context,
    testTag: String
) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .height(56.dp)
              .background(Color(0xFFEBF1F4))
              .clickable {
                if (text == " Share to" && bitmap != null) {
                  val photoFile = File(context.cacheDir, "shared_image.jpg")
                  val outputStream = FileOutputStream(photoFile)
                  bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                  outputStream.flush()
                  outputStream.close()

                  val uri =
                      FileProvider.getUriForFile(
                          context, "${context.packageName}.fileprovider", photoFile)

                  val shareIntent =
                      Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, uri)
                        type = "image/jpeg"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                      }

                  context.startActivity(Intent.createChooser(shareIntent, "Share image via"))
                }
              }
              .padding(horizontal = 16.dp)
              .testTag(testTag),
      verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = text,
            modifier = Modifier.size(24.dp).padding(end = 8.dp),
            tint = Color.Gray)
        Text(
            text = text,
            color = Color.Gray,
            fontSize = 18.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Start,
        )
      }
}
