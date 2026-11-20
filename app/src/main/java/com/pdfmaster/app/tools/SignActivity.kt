package com.pdfmaster.app.tools

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pdfmaster.app.databinding.ActivitySignBinding
import com.pdfmaster.app.utils.FileUtils
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class SignActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignBinding
    private var inputUri: Uri? = null

    private val pickPdf = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            inputUri = uri
            binding.tvFile.text = "File: ${uri.lastPathSegment ?: "PDF"}"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Sign PDF"

        binding.btnPick.setOnClickListener { pickPdf.launch("application/pdf") }
        binding.btnClear.setOnClickListener { binding.signatureView.clear() }
        binding.btnSign.setOnClickListener { doSign() }
    }

    private fun doSign() {
        val uri = inputUri ?: run {
            Toast.makeText(this, "Pick a PDF first", Toast.LENGTH_SHORT).show(); return
        }
        if (binding.signatureView.isEmpty()) {
            Toast.makeText(this, "Draw signature first", Toast.LENGTH_SHORT).show(); return
        }
        binding.btnSign.isEnabled = false
        binding.tvStatus.text = "Signing..."

        val sigBitmap = binding.signatureView.getBitmap()

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { signPdf(uri, sigBitmap) }
                binding.tvStatus.text = "✅ Saved to Downloads"
                Toast.makeText(this@SignActivity, "Saved", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                binding.tvStatus.text = "Error: ${e.message}"
            } finally {
                binding.btnSign.isEnabled = true
            }
        }
    }

    private fun signPdf(uri: Uri, sigBitmap: android.graphics.Bitmap) {
        val input = FileUtils.copyUriToCache(this, uri)
        val sigFile = File(cacheDir, "sig_${System.currentTimeMillis()}.png")
        FileOutputStream(sigFile).use { sigBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }

        val output = File(cacheDir, "signed_${System.currentTimeMillis()}.pdf")
        PDDocument.load(input).use { doc ->
            val lastPage = doc.getPage(doc.numberOfPages - 1)
            val pdImage = PDImageXObject.createFromFile(sigFile.absolutePath, doc)
            val width = 200f
            val height = (pdImage.height.toFloat() / pdImage.width) * width
            PDPageContentStream(doc, lastPage, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                cs.drawImage(pdImage, lastPage.mediaBox.width - width - 20f, 20f, width, height)
            }
            doc.save(output)
        }
        val finalName = "PDFMaster_signed_${System.currentTimeMillis()}.pdf"
        FileUtils.saveToDownloads(this, output, finalName)
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
