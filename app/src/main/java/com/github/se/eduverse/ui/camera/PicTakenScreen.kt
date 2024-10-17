package com.github.se.eduverse.ui.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.se.eduverse.R
import com.github.se.eduverse.model.Photo
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.PhotoViewModel
import com.google.firebase.auth.FirebaseAuth
import java.io.ByteArrayOutputStream
import java.io.File

fun adjustImageRotation(bitmap: Bitmap): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(90f)
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

@Composable
fun PicTakenScreen(
    photoFile: File?,
    navigationActions: NavigationActions,
    viewModel: PhotoViewModel
) {
    val auth = FirebaseAuth.getInstance()
    val ownerId = auth.currentUser?.uid ?: "anonymous"
    val path = "photos/$ownerId/${System.currentTimeMillis()}.jpg"

    val bitmap = photoFile?.let {
        BitmapFactory.decodeFile(it.path)?.let { adjustImageRotation(it) }?.asImageBitmap()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEBF1F4)) // Set background color to #EBF1F4
            .padding(8.dp)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Captured Photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(12.dp)) // Slightly rounded corners
                    .background(Color.LightGray)
                    .testTag("capturedImage")
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.google_logo),
                contentDescription = "Google Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(12.dp)) // Slightly rounded corners
                    .background(Color.LightGray)
                    .testTag("googleLogoImage")
            )
        }

        Column(
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 40.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Using im1.png for Crop Icon
            Image(
                painter = painterResource(id = R.drawable.vector),  // Replace with the correct resource ID for im1.png
                contentDescription = "Crop Photo",
                modifier = Modifier
                    .size(30.dp)
                    .clickable {}
                    .testTag("cropIcon")
            )
            // Using im2.png for Filters Icon
            Image(
                painter = painterResource(id = R.drawable.settings),  // Replace with the correct resource ID for im2.png
                contentDescription = "Filters",
                modifier = Modifier
                    .size(40.dp)
                    .clickable {}
                    .testTag("filterIcon")
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    bitmap?.let {
                        val byteArray = imageBitmapToByteArray(it)
                        val photo = Photo(ownerId, byteArray, path)
                        viewModel.savePhoto(photo)
                        navigationActions.goBack()
                        navigationActions.goBack()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("saveButton"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC6D3E1)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Save",
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight(500),
                        color = Color(0xFF4A4459),
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Button(
                onClick = {
                    val encodedPath = photoFile?.absolutePath?.let { Uri.encode(it) }
                    encodedPath?.let { navigationActions.navigateTo("nextScreen/$it") }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("nextButton"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37CED5)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Next",
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight(500),
                        color = Color(0xFF4A4459),
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(40.dp)
                .clickable { navigationActions.goBack() }
                .padding(8.dp)
                .testTag("closeButton")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(32.dp).align(Alignment.Center)
            )
        }
    }
}

fun imageBitmapToByteArray(imageBitmap: ImageBitmap): ByteArray {
    val bitmap = imageBitmap.asAndroidBitmap()
    val byteArrayOutputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
    return byteArrayOutputStream.toByteArray()
}
