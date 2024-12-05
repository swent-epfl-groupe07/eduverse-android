/** Extensions.kt - A Kotlin file containing extension functions useful for the application */
package com.github.se.eduverse

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast

/**
 * Extension function to show a toast message in the context
 *
 * @param message Message to be displayed in the toast
 * @param duration Duration for which the toast should be displayed
 */
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
  Toast.makeText(this, message, duration).show()
}

/**
 * Extension function to check if the device is connected to the internet
 *
 * @return True if the device is connected to the internet, false otherwise
 */
fun Context.isNetworkAvailable(): Boolean {
  val connectivityManager =
      this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
  val activeNetwork = connectivityManager.activeNetwork
  val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

  return networkCapabilities != null &&
      networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
