package com.github.se.eduverse.ui.converter

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.github.se.eduverse.ui.navigation.NavigationActions


@Composable
fun PdfConverterScreen(navigationActions: NavigationActions) {
  Scaffold(floatingActionButton = {
      FloatingActionButton(
          onClick = { /*TODO*/ },
          content = { Icon(Icons.Filled.Add, contentDescription = "Add new pdf") }
      )
  }) { pd ->
    Column(modifier = Modifier.padding(pd)){

    }

}}
