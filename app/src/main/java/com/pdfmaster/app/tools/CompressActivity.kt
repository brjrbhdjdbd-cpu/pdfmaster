package com.pdfmaster.app.tools

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pdfmaster.app.databinding.ActivityCompressBinding
import com.pdfmaster.app.utils.FileUtils
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class CompressActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompressBinding
    private var inputUri: Uri? = null

    private val pickPdf = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            inputUri = uri
            val size = contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0
            binding.tvFileInfo.text = "Selected file\nSize: ${FileUtils.formatSize(size)}"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompressBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Compress PDF"

        binding.btnPick.setOnClickListener { pickPdf.launch("application/pdf") }
        binding.btnCompress.setOnClickListener { startCompress() }
    }

    private fun startCompress() {
        val uri = inputUri ?: run {
            Toast.makeText(this, "Pick a file first", Toast.LENGTH_SHORT).show()
            return
        }
        binding.btnCompress.isEnabled = false
        binding.tvResult.text = "Compressing..."
        val quality = binding.sliderQuality.progress

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { compressPdf(uri, quality) }
                binding.tvResult.text = "Original: ${FileUtils.formatSize(result.first)}\n" +
                        "Compressed: ${FileUtils.formatSize(result.second)}\n" +
                        "Saved: ${FileUtils.formatSize(result.first - result.second)}"
                Toast.makeText(this@CompressActivity, "Saved to Downloads", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                binding.tvResult.text = "Error: ${e.message}"
                Toast.makeText(this@CompressActivity, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.btnCompress.isEnabled = true
            }
        }
    }

    private fun compressPdf(uri: Uri, quality: Int): Pair<Long, Long> {
        val input = FileUtils.copyUriToCache(this, uri)
        val originalSize = input.length()
        val output = File(cacheDir, "compressed_${System.currentTimeMillis()}.pdf")

        PDDocument.load(input).use { doc ->
            for (page: PDPage in doc.pages) {
                val resources = page.resources ?: continue
                for (name in resources.xObjectNames) {
                    val obj = resources.getXObject(name)
                    if (obj is PDImageXObject) {
                        try {
                            val img = obj.image ?: continue
                            if (img.width < 100 || img.height < 100) continue
                            // Recompress image as JPEG
                            val bos = java.io.ByteArrayOutputStream()
                            img.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, bos)
                        } catch (_: Exception) { }
                    }
                }
            }
            doc.save(output)
        }

        val finalName = "PDFMaster_compressed_${System.currentTimeMillis()}.pdf"
        FileUtils.saveToDownloads(this, output, finalName)
        return originalSize to output.length()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
