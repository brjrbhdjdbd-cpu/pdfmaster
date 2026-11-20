package com.pdfmaster.app.viewer

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pdfmaster.app.databinding.ActivityViewerBinding
import java.io.File
import java.io.FileOutputStream

class ViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "PDF Viewer"

        val uriString = intent.getStringExtra("pdf_uri")
        if (uriString == null) {
            Toast.makeText(this, "No PDF provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        try {
            val uri = Uri.parse(uriString)
            val input = contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Cannot open stream")

            val cacheFile = File(cacheDir, "temp_view.pdf")
            FileOutputStream(cacheFile).use { output ->
                input.copyTo(output)
            }
            input.close()

            binding.pdfView
                .fromFile(cacheFile)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .defaultPage(0)
                .enableAnnotationRendering(true)
                .onError { t ->
                    Toast.makeText(this, "Error: ${t.message}", Toast.LENGTH_LONG).show()
                }
                .load()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
