package com.pdfmaster.app.tools

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pdfmaster.app.databinding.ActivityMergeBinding
import com.pdfmaster.app.utils.FileUtils
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MergeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMergeBinding
    private val selected = mutableListOf<Uri>()

    private val pickPdf = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selected.add(uri)
            refreshList()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMergeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Merge PDFs"

        binding.btnAdd.setOnClickListener { pickPdf.launch("application/pdf") }
        binding.btnMerge.setOnClickListener { doMerge() }
    }

    private fun refreshList() {
        val sb = StringBuilder()
        selected.forEachIndexed { i, uri ->
            sb.append("${i + 1}. ${uri.lastPathSegment ?: "PDF"}\n")
        }
        if (selected.isEmpty()) sb.append("No files selected\n\nUse \"Add PDF\" to pick files")
        binding.tvFiles.text = sb.toString()
        binding.btnMerge.isEnabled = selected.size >= 2
    }

    private fun doMerge() {
        if (selected.size < 2) {
            Toast.makeText(this, "Select at least 2 files", Toast.LENGTH_SHORT).show()
            return
        }
        binding.btnMerge.isEnabled = false
        binding.tvStatus.text = "Merging..."

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { mergePdfs() }
                binding.tvStatus.text = "✅ Saved to Downloads"
                Toast.makeText(this@MergeActivity, "Saved to Downloads", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                binding.tvStatus.text = "Error: ${e.message}"
                Toast.makeText(this@MergeActivity, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.btnMerge.isEnabled = true
            }
        }
    }

    private fun mergePdfs() {
        val merger = PDFMergerUtility()
        val output = File(cacheDir, "merged_${System.currentTimeMillis()}.pdf")
        merger.destinationFileName = output.absolutePath
        selected.forEachIndexed { i, uri ->
            val f = FileUtils.copyUriToCache(this, uri, "merge_${i}_${System.currentTimeMillis()}.pdf")
            merger.addSource(f)
        }
        merger.mergeDocuments(MemoryUsageSetting.setupTempFileOnly())
        val finalName = "PDFMaster_merged_${System.currentTimeMillis()}.pdf"
        FileUtils.saveToDownloads(this, output, finalName)
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
