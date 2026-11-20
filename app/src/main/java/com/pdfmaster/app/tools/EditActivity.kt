package com.pdfmaster.app.tools

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pdfmaster.app.databinding.ActivityEditBinding
import com.pdfmaster.app.utils.FileUtils
import com.pdfmaster.app.utils.PageSelection
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class EditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditBinding
    private var inputUri: Uri? = null
    private var pageCount: Int = 0

    private val pickPdf = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            inputUri = uri
            loadInfo(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Edit PDF"

        binding.btnPick.setOnClickListener { pickPdf.launch("application/pdf") }
        binding.btnRotate.setOnClickListener { doRotate() }
        binding.btnDelete.setOnClickListener { doDelete() }
        binding.btnAddText.setOnClickListener { doAddText() }
        binding.btnAddBlank.setOnClickListener { doAddBlankPage() }
    }

    private fun loadInfo(uri: Uri) {
        lifecycleScope.launch {
            try {
                pageCount = withContext(Dispatchers.IO) {
                    val f = FileUtils.copyUriToCache(this@EditActivity, uri, "edit_load.pdf")
                    PDDocument.load(f).use { it.numberOfPages }
                }
                binding.tvFile.text = "File: ${uri.lastPathSegment ?: "PDF"}\nPages: $pageCount"
            } catch (e: Exception) {
                binding.tvFile.text = "Error loading: ${e.message}"
            }
        }
    }

    private fun doRotate() {
        val uri = inputUri ?: return toast("Pick a PDF first")
        val pages = PageSelection.parse(binding.etPages.text.toString(), pageCount)
            ?: return toast("Invalid page numbers (max $pageCount)")
        val deg = binding.sliderRotation.value.toInt() * 90
        val angle = when (deg) {
            0 -> 90
            1 -> 90
            2 -> 180
            3 -> 270
            else -> 90
        } // fallback default 90

        runOp("Rotating...") { input ->
            PDDocument.load(input).use { doc ->
                pages.forEach { p ->
                    val page = doc.getPage(p - 1)
                    page.rotation = (page.rotation + angle) % 360
                }
                save(doc, "PDFMaster_rotated_${System.currentTimeMillis()}.pdf")
            }
        }
    }

    private fun doDelete() {
        val uri = inputUri ?: return toast("Pick a PDF first")
        val pages = PageSelection.parse(binding.etPages.text.toString(), pageCount)
            ?: return toast("Invalid page numbers")
        if (pages.size >= pageCount) return toast("Cannot delete all pages")

        runOp("Deleting...") { input ->
            PDDocument.load(input).use { doc ->
                pages.sortedDescending().forEach { p -> doc.removePage(p - 1) }
                save(doc, "PDFMaster_deleted_${System.currentTimeMillis()}.pdf")
            }
        }
    }

    private fun doAddText() {
        val uri = inputUri ?: return toast("Pick a PDF first")
        val text = binding.etText.text.toString()
        if (text.isBlank()) return toast("Enter text to add")
        val pages = PageSelection.parse(binding.etPages.text.toString(), pageCount)
            ?: return toast("Invalid page numbers")

        runOp("Adding text...") { input ->
            PDDocument.load(input).use { doc ->
                pages.forEach { p ->
                    val page = doc.getPage(p - 1)
                    PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                        cs.beginText()
                        cs.setFont(PDType1Font.HELVETICA_BOLD, 18f)
                        cs.newLineAtOffset(50f, page.mediaBox.height - 60f)
                        cs.showText(sanitize(text))
                        cs.endText()
                    }
                }
                save(doc, "PDFMaster_text_${System.currentTimeMillis()}.pdf")
            }
        }
    }

    private fun doAddBlankPage() {
        val uri = inputUri ?: return toast("Pick a PDF first")
        val text = binding.etText.text.toString().ifBlank { "New Page" }

        runOp("Adding page...") { input ->
            PDDocument.load(input).use { doc ->
                val newPage = PDPage(PDRectangle.A4)
                doc.addPage(newPage)
                PDPageContentStream(doc, newPage).use { cs ->
                    cs.beginText()
                    cs.setFont(PDType1Font.HELVETICA, 24f)
                    cs.newLineAtOffset(72f, newPage.mediaBox.height - 100f)
                    cs.showText(sanitize(text))
                    cs.endText()
                }
                save(doc, "PDFMaster_newpage_${System.currentTimeMillis()}.pdf")
            }
        }
    }

    private fun runOp(status: String, op: (File) -> Unit) {
        val uri = inputUri ?: return
        binding.tvStatus.text = status
        binding.btnRotate.isEnabled = false
        binding.btnDelete.isEnabled = false
        binding.btnAddText.isEnabled = false
        binding.btnAddBlank.isEnabled = false

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val input = FileUtils.copyUriToCache(this@EditActivity, uri, "edit_input.pdf")
                    op(input)
                }
                binding.tvStatus.text = "✅ Saved to Downloads"
                toast("Saved to Downloads")
            } catch (e: Exception) {
                binding.tvStatus.text = "Error: ${e.message}"
                toast("Failed: ${e.message}")
            } finally {
                binding.btnRotate.isEnabled = true
                binding.btnDelete.isEnabled = true
                binding.btnAddText.isEnabled = true
                binding.btnAddBlank.isEnabled = true
            }
        }
    }

    private fun save(doc: PDDocument, name: String) {
        val out = File(cacheDir, name)
        doc.save(out)
        FileUtils.saveToDownloads(this, out, name)
    }

    private fun sanitize(s: String): String {
        // Helvetica doesn't support Arabic. Keep ASCII, replace unsupported chars.
        return s.map { if (it.code in 32..126) it else '?' }.joinToString("")
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
