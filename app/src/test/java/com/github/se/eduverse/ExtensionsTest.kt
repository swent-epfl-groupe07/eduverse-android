package com.github.se.eduverse

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class ExtensionsTest {
  @Test
  fun `isNetworkAvailable returns true when device is connected`() {
    val context = mock(Context::class.java)
    val connectivityManager = mock(ConnectivityManager::class.java)
    val network = mock(Network::class.java)
    val networkCapabilities = mock(NetworkCapabilities::class.java)

    `when`(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager)
    `when`(connectivityManager.activeNetwork).thenReturn(network)
    `when`(connectivityManager.getNetworkCapabilities(network)).thenReturn(networkCapabilities)
    `when`(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(true)

    val result = context.isNetworkAvailable()

    assertTrue(result)
  }

  @Test
  fun `isNetworkAvailable returns false when device is not connected`() {
    val context = mock(Context::class.java)
    val connectivityManager = mock(ConnectivityManager::class.java)

    `when`(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager)
    `when`(connectivityManager.activeNetwork).thenReturn(null) // Simulate no active network

    val result = context.isNetworkAvailable()

    assertFalse(result)
  }
}
