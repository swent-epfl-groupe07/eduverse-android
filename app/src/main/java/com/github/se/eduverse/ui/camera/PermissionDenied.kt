import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION

@Composable
fun PermissionDeniedScreen(navigationActions: NavigationActions) {
  Scaffold(
    bottomBar = {
      BottomNavigationMenu(
        onTabSelect = { route -> navigationActions.navigateTo(route) },
        tabList = LIST_TOP_LEVEL_DESTINATION,
        selectedItem = navigationActions.currentRoute())
    },
    content = { padding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "Camera permission is required to use this feature.",
          modifier = Modifier.testTag("permissionText")
        )
      }
    }
  )
}


