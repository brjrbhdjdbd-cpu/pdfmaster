package com.pdfmaster.app.tools

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.pdfmaster.app.databinding.ActivityOcrBinding
import com.pdfmaster.app.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

class OcrActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOcrBinding
    private var inputUri: Uri? = null

    private val pickPdf = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            inputUri = uri
            binding.tvFile.text = "File: ${uri.lastPathSegment ?: "PDF"}"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOcrBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "OCR — Extract Text"

        binding.btnPick.setOnClickListener { pickPdf.launch("application/pdf") }
        binding.btnExtract.setOnClickListener { runOcr() }
    }

    private fun runOcr() {
        val uri = inputUri ?: run {
            Toast.makeText(this, "Pick a PDF first", Toast.LENGTH_SHORT).show(); return
        }
        binding.btnExtract.isEnabled = false
        binding.tvResult.text = "Extracting... please wait"
        binding.progress.visibility = android.view.View.VISIBLE

        lifecycleScope.launch {
            try {
                val text = withContext(Dispatchers.IO) { extractText(uri) }
                binding.tvResult.text = if (text.isBlank()) "No text found in PDF" else text
            } catch (e: Exception) {
                binding.tvResult.text = "Error: ${e.message}"
            } finally {
                binding.btnExtract.isEnabled = true
                binding.progress.visibility = android.view.View.GONE
            }
        }
    }

    private suspend fun extractText(uri: Uri): String {
        val file = FileUtils.copyUriToCache(this, uri)
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val sb = StringBuilder()

        for (i in 0 until renderer.pageCount) {
            val page = renderer.openPage(i)
            val scale = 2
            val bmp = Bitmap.createBitmap(page.width * scale, page.height * scale, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(android.graphics.Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            val image = InputImage.fromBitmap(bmp, 0)
            val result = recognizer.process(image).await()
            sb.append("═══ Page ${i + 1} ═══\n\n")
            sb.append(result.text.ifBlank { "(no text)" })
            sb.append("\n\n")
            bmp.recycle()
        }
        renderer.close()
        return sb.toString()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
