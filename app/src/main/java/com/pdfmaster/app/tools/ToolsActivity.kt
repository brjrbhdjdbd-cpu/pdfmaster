package com.pdfmaster.app.tools

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pdfmaster.app.databinding.ActivityToolsBinding

class ToolsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityToolsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityToolsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Tools"

        binding.cardCompress.setOnClickListener {
            Toast.makeText(this, "Compress: coming soon", Toast.LENGTH_SHORT).show()
        }
        binding.cardMerge.setOnClickListener {
            Toast.makeText(this, "Merge: coming soon", Toast.LENGTH_SHORT).show()
        }
        binding.cardSign.setOnClickListener {
            Toast.makeText(this, "Sign: coming soon", Toast.LENGTH_SHORT).show()
        }
        binding.cardOcr.setOnClickListener {
            Toast.makeText(this, "OCR: coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
