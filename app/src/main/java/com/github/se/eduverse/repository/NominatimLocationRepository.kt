package com.github.se.eduverse.repository

import android.util.Log
import com.github.se.eduverse.model.Location
import java.io.IOException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException

class NominatimLocationRepository(val client: OkHttpClient) : LocationRepository {

    override fun search(
        query: String,
        onSuccess: (List<Location>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val url = "https://nominatim.openstreetmap.org/search?format=json&q=$query"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "EduverseApp")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onFailure(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { body ->
                    val json = body.string()
                    try {
                        val locations = parseLocationJson(json)
                        onSuccess(locations)
                    } catch (e: JSONException) {
                        onFailure(e)
                    }
                } ?: onFailure(IOException("Response body is null"))
            }
        })
    }

    suspend fun parseLocation(address: String): Location? {
        val url = "https://nominatim.openstreetmap.org/search?format=json&q=${address.replace(" ", "+")}"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "EduverseApp")
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("Failed to fetch location: ${response.message}")

            val responseBody = response.body?.string()
                ?: throw Exception("Empty response from Nominatim")

            val jsonArray = JSONArray(responseBody)

            if (jsonArray.length() == 0) return null

            val firstResult = jsonArray.getJSONObject(0)
            val latitude = firstResult.getString("lat").toDouble()
            val longitude = firstResult.getString("lon").toDouble()
            val displayName = firstResult.getString("display_name")

            Location(latitude = latitude, longitude = longitude, name = displayName)
        } catch (e: Exception) {
            Log.e("PARSE_LOCATION", "Failed to parse location: ${e.message}")
            null
        }
    }


    private fun parseLocationJson(json: String): List<Location> {
        val locations = mutableListOf<Location>()
        val jsonArray = JSONArray(json)

        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val latitude = jsonObject.getString("lat").toDouble()
            val longitude = jsonObject.getString("lon").toDouble()
            val displayName = jsonObject.getString("display_name")

            val location = Location(latitude, longitude, displayName)
            locations.add(location)
        }

        return locations
    }
}