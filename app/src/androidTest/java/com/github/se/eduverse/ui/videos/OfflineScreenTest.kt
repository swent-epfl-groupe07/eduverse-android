package com.github.se.eduverse.ui.offline

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.annotation.ExperimentalCoilApi
import com.google.accompanist.pager.ExperimentalPagerApi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

@RunWith(AndroidJUnit4::class)
class OfflineScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockGetCachedFiles: (Context) -> List<File> = { emptyList() }
    private val mockCheckConnection: (Context) -> Boolean = { false }

    @Test
    fun testNoOfflineMediaDisplayed() {
        composeTestRule.setContent {
            OfflineScreen(
                onGoBack = {},
                getCachedFilesFun = { emptyList() },
                checkConnectionFun = { false }
            )
        }

        composeTestRule.onNodeWithTag("NoOfflineMediaText").assertIsDisplayed()

        composeTestRule.onNodeWithTag("TopAppBar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("BackButton").assertIsDisplayed()

        composeTestRule.onNodeWithTag("OfflineMediaTitle").assertIsDisplayed()
    }

    @Test
    fun testOfflineMediaImageDisplayed() {
        val fakeImageFile = File("test.jpg")
        composeTestRule.setContent {
            OfflineScreen(
                onGoBack = {},
                getCachedFilesFun = { listOf(fakeImageFile) },
                checkConnectionFun = { false }
            )
        }

        composeTestRule.onNodeWithTag("MediaPager").assertIsDisplayed()
        composeTestRule.onNodeWithTag("CachedImageDisplay").assertIsDisplayed()
        composeTestRule.onNodeWithTag("LikeButton").assertIsDisplayed()
        composeTestRule.onNodeWithTag("CommentButton").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ShareButton").assertIsDisplayed()
    }

    @Test
    fun testOfflineMediaVideoDisplayed() {
        val fakeVideoFile = File("test.mp4")
        composeTestRule.setContent {
            OfflineScreen(
                onGoBack = {},
                getCachedFilesFun = { listOf(fakeVideoFile) },
                checkConnectionFun = { false }
            )
        }

        composeTestRule.onNodeWithTag("MediaPager").assertIsDisplayed()
        composeTestRule.onNodeWithTag("CachedVideoPlayer").assertIsDisplayed()
    }

    @Test
    fun testBackButtonClick() {
        var backClicked = false
        composeTestRule.setContent {
            OfflineScreen(
                onGoBack = { backClicked = true },
                getCachedFilesFun = { emptyList() },
                checkConnectionFun = { false }
            )
        }
        composeTestRule.onNodeWithTag("BackButton").performClick()
        assert(backClicked)
    }

    @Test
    fun testActionsWhenOffline() {
        val fakeVideoFile = File("test.mp4")
        composeTestRule.setContent {
            OfflineScreen(
                onGoBack = {},
                getCachedFilesFun = { listOf(fakeVideoFile) },
                checkConnectionFun = { false }
            )
        }

        composeTestRule.onNodeWithTag("LikeButton").performClick()
        composeTestRule.onNodeWithTag("CommentButton").performClick()
        composeTestRule.onNodeWithTag("ShareButton").performClick()
    }

    @Test
    fun testActionsWhenOnline() {
        val fakeVideoFile = File("test.mp4")
        composeTestRule.setContent {
            OfflineScreen(
                onGoBack = {},
                getCachedFilesFun = { listOf(fakeVideoFile) },
                checkConnectionFun = { true }
            )
        }

        composeTestRule.onNodeWithTag("LikeButton").performClick()
        composeTestRule.onNodeWithTag("CommentButton").performClick()
        composeTestRule.onNodeWithTag("ShareButton").performClick()
    }

    @Test
    fun testDoubleTapOffline() {
        val fakeVideoFile = File("test.mp4")
        composeTestRule.setContent {
            OfflineScreen(
                onGoBack = {},
                getCachedFilesFun = { listOf(fakeVideoFile) },
                checkConnectionFun = { false }
            )
        }

        composeTestRule.onNodeWithTag("MediaItem").performGesture { doubleClick(center) }
    }

    @Test
    fun testDoubleTapOnline() {
        val fakeVideoFile = File("test.mp4")
        composeTestRule.setContent {
            OfflineScreen(
                onGoBack = {},
                getCachedFilesFun = { listOf(fakeVideoFile) },
                checkConnectionFun = { true }
            )
        }

        composeTestRule.onNodeWithTag("MediaItem").performGesture { doubleClick(center) }
    }
}
