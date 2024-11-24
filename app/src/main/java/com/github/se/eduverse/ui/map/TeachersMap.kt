package com.github.se.eduverse.ui.map

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.lang.reflect.Modifier

@Composable
fun MapScreen(
    listToDosViewModel: ListToDosViewModel = viewModel(factory = ListToDosViewModel.Factory),
    navigationActions: NavigationActions
) {
    val toDosWithLocation by listToDosViewModel.todos.collectAsState(initial = emptyList())

    val initialPosition = LatLng(46.5191, 6.63)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 10f)
    }

    Scaffold(
        bottomBar = {
            BottomNavigationMenu(
                onTabSelect = { route -> navigationActions.navigateTo(route) },
                tabList = LIST_TOP_LEVEL_DESTINATION,
                selectedItem = navigationActions.currentRoute())
        },
        content = { pd ->
            GoogleMap(
                modifier = Modifier.padding(pd).testTag("mapScreen"),
                cameraPositionState = cameraPositionState) {
                toDosWithLocation.forEach { todo ->
                    val location = todo.location?.let { LatLng(it.latitude, todo.location.longitude) }

                    location
                        ?.let { MarkerState(position = it) }
                        ?.let {
                            Marker(
                                state = it,
                                title = todo.name,
                                snippet = todo.description,
                                onClick = {
                                    location
                                        ?.let { it1 -> CameraUpdateFactory.newLatLngZoom(it1, 15f) }
                                        ?.let { it2 -> cameraPositionState.move(it2) }
                                    false
                                })
                        }
                }
            }
        })
}