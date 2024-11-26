package com.github.se.eduverse.repository

import com.github.se.eduverse.api.FileConversionException
import com.github.se.eduverse.api.FileDownloadException
import io.mockk.every
import io.mockk.mockk
import java.io.File
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConvertApiRepositoryTest {

  private lateinit var convertApiRepository: ConvertApiRepository
  private lateinit var mockClient: OkHttpClient
  private lateinit var mockConversionCall: Call
  private lateinit var mockDownloadCall: Call
  private lateinit var mockConversionResponse: Response
  private lateinit var mockDownloadResponse: Response

  @Before
  fun setUp() {
    mockClient = mockk()
    mockConversionCall = mockk()
    mockDownloadCall = mockk()
    mockConversionResponse = mockk()
    mockDownloadResponse = mockk()

    convertApiRepository = ConvertApiRepository(mockClient)

    every { mockClient.newCall(any()) } answers
        {
          val request = it.invocation.args[0] as Request
          if (request.url.toString().contains("/convert/")) mockConversionCall else mockDownloadCall
        }
    every { mockConversionCall.execute() } returns mockConversionResponse
    every { mockDownloadCall.execute() } returns mockDownloadResponse
  }

  @Test
  fun `convertToPdf should throw IllegalArgumentException for unsupported file type`() {
    val file = File.createTempFile("test", ".pdf")
    val pdfName = "testPdf"
    assertThrows(IllegalArgumentException::class.java) {
      convertApiRepository.convertToPdf(file, pdfName)
    }
    file.delete()
  }

  @Test
  fun `convertToPdf should throw FileConversionException when response is not successful`() {
    val file = File.createTempFile("test", ".docx")
    val pdfName = "testPdf"

    every { mockConversionResponse.isSuccessful } returns false

    assertThrows(FileConversionException::class.java) {
      convertApiRepository.convertToPdf(file, pdfName)
    }
    file.delete()
  }

  @Test
  fun `convertToPdf should throw FileConversionException when response body is empty`() {
    val file = File.createTempFile("test", ".docx")
    val pdfName = "testPdf"

    every { mockConversionResponse.isSuccessful } returns true
    every { mockConversionResponse.body } returns null

    assertThrows(FileConversionException::class.java) {
      convertApiRepository.convertToPdf(file, pdfName)
    }
    file.delete()
  }

  @Test
  fun `convertToPdf should throw FileConversionException when file URL is not found in response`() {
    val file = File.createTempFile("test", ".docx")
    val pdfName = "testPdf"
    val mockResponseBody = """{"Files":[]}""".toResponseBody("application/json".toMediaType())

    every { mockConversionResponse.isSuccessful } returns true
    every { mockConversionResponse.body } returns mockResponseBody
    assertThrows(FileConversionException::class.java) {
      convertApiRepository.convertToPdf(file, pdfName)
    }
    file.delete()
  }

  @Test
  fun `convertToPdf should throw FileDownloadException when download response is not successful`() {
    val file = File.createTempFile("test", ".docx")
    val pdfName = "testPdf"

    val mockConversionResponseBody =
        """{"Files":[{"Url":"https://example.com/file.pdf"}]}"""
            .toResponseBody("application/json".toMediaType())

    every { mockConversionResponse.isSuccessful } returns true
    every { mockConversionResponse.body } returns mockConversionResponseBody

    every { mockDownloadResponse.isSuccessful } returns false

    assertThrows(FileDownloadException::class.java) {
      convertApiRepository.convertToPdf(file, pdfName)
    }
    file.delete()
  }

  @Test
  fun `convertToPdf should throw FileDownloadException when download response body is empty`() {
    val file = File.createTempFile("test", ".docx")
    val pdfName = "testPdf"

    val mockConversionResponseBody =
        """{"Files":[{"Url":"https://example.com/file.pdf"}]}"""
            .toResponseBody("application/json".toMediaType())

    every { mockConversionResponse.isSuccessful } returns true
    every { mockConversionResponse.body } returns mockConversionResponseBody

    every { mockDownloadResponse.isSuccessful } returns true
    every { mockDownloadResponse.body } returns null

    assertThrows(FileDownloadException::class.java) {
      convertApiRepository.convertToPdf(file, pdfName)
    }
    file.delete()
  }

  @Test
  fun `convertToPdf should return converted PDF file when successful`() {
    val file = File.createTempFile("test", ".docx")
    val pdfName = "testPdf"

    val mockConversionResponseBody =
        """{"Files":[{"Url":"https://example.com/file.pdf"}]}"""
            .toResponseBody("application/json".toMediaType())

    every { mockConversionResponse.isSuccessful } returns true
    every { mockConversionResponse.body } returns mockConversionResponseBody

    val mockDownloadResponseBody =
        "Mock PDF content".toResponseBody("application/pdf".toMediaType())

    every { mockDownloadResponse.isSuccessful } returns true
    every { mockDownloadResponse.body } returns mockDownloadResponseBody

    val result = convertApiRepository.convertToPdf(file, pdfName)

    assertNotNull(result)
    assertTrue(result!!.exists())
    assertTrue(result.name.startsWith(pdfName) && result.extension == "pdf")
    file.delete()
    result.delete()
  }
}
