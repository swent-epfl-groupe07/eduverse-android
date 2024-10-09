//This file was copied and adapted from the bootcamp solution

package com.github.se.eduverse.ui.navigation

//noinspection UsingMaterialAndMaterial3Libraries
//noinspection UsingMaterialAndMaterial3Libraries

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun BottomNavigationMenu(
    onTabSelect: (TopLevelDestination) -> Unit,
    tabList: List<TopLevelDestination>,
    selectedItem: String
) {
    BottomNavigation(
        modifier = Modifier.fillMaxWidth().height(60.dp).testTag("bottomNavigationMenu"),
        backgroundColor = MaterialTheme.colorScheme.surface,
        content = {
            tabList.forEach { tab ->
                BottomNavigationItem(
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = { Text(tab.textId) },
                    selected = tab.route == selectedItem,
                    onClick = { onTabSelect(tab) },
                    modifier = Modifier.clip(RoundedCornerShape(50.dp)).testTag(tab.textId))
            }
        },
    )
}