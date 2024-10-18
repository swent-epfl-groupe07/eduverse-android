/** Extensions.kt - A Kotlin file containing extension functions useful for the application */
package com.github.se.eduverse

import android.content.Context
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
