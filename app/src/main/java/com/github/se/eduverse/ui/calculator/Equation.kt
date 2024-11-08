package com.github.se.eduverse.ui.calculator

import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.JsonParser

class Equation {

    private val apiKey = "7GRQRL-485HEKL5W8"

    fun solveEquation(equation: String): String {
        val encodedEquation = java.net.URLEncoder.encode("solve $equation for x", "UTF-8")
        val url = "http://api.wolframalpha.com/v2/query?input=$encodedEquation&format=plaintext&output=JSON&appid=$apiKey"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("network error: ${response.message}")

                val jsonData = response.body?.string()
                if (jsonData != null) {
                    println("JSON brut : $jsonData")
                    val solution = parseSolutionFromWolframJson(jsonData)
                    solution ?: "No solution found"
                } else {
                    "Error: Empty response"
                }
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private fun parseSolutionFromWolframJson(jsonData: String): String? {
        try {
            val jsonObject = JsonParser.parseString(jsonData).asJsonObject
            val queryResult = jsonObject.getAsJsonObject("queryresult")

            if (!queryResult.get("success").asBoolean) {
                return "Query was unsuccessful"
            }

            val pods = queryResult.getAsJsonArray("pods")
            val realSolutions = mutableListOf<String>()
            var solutionIndex = 1

            for (pod in pods) {
                val podObject = pod.asJsonObject
                val podTitle = podObject.get("title").asString

                if (podTitle.contains("Result", ignoreCase = true) ||
                    podTitle.contains("Solution", ignoreCase = true) ||
                    podTitle.contains("Root", ignoreCase = true)
                ) {
                    val subpods = podObject.getAsJsonArray("subpods")

                    for (subpod in subpods) {
                        val solutionText = subpod.asJsonObject.get("plaintext")?.asString ?: continue
                        extractRealSolutions(solutionText, realSolutions, solutionIndex)
                        solutionIndex = realSolutions.size + 1
                    }
                }
            }

            return if (realSolutions.isNotEmpty()) realSolutions.joinToString(", ") else "No real solutions found"
        } catch (e: Exception) {
            println("Error parsing JSON: ${e.message}")
            return "Error parsing solution"
        }
    }

    private fun extractRealSolutions(solutionText: String, realSolutions: MutableList<String>, startIndex: Int) {
        var solutionIndex = startIndex
        if (!solutionText.contains("i")) {
            when {
                solutionText.contains("±") -> {
                    val parts = solutionText.split("±")
                    realSolutions.add("x${solutionIndex++} = ${parts[0].trim().removePrefix("x =")} + ${parts[1].trim()}")
                    realSolutions.add("x${solutionIndex++} = ${parts[0].trim().removePrefix("x =")} - ${parts[1].trim()}")
                }
                solutionText.contains(" or ") -> {
                    val parts = solutionText.split(" or ")
                    for (part in parts) {
                        realSolutions.add("x${solutionIndex++} = ${part.trim().removePrefix("x =")}")
                    }
                }
                else -> {
                    realSolutions.add("x${solutionIndex++} = ${solutionText.trim().removePrefix("x =")}")
                }
            }
        }
    }
}


fun main() {
    val Equation = Equation()
    val equation = "x^2 + 3x = 0"
    val solution = Equation.solveEquation(equation)
    println("$solution")
}
