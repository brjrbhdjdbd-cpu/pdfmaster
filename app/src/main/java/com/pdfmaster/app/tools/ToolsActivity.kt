package com.pdfmaster.app.tools

import android.content.Intent
import android.os.Bundle
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
            startActivity(Intent(this, CompressActivity::class.java))
        }
        binding.cardMerge.setOnClickListener {
            startActivity(Intent(this, MergeActivity::class.java))
        }
        binding.cardSign.setOnClickListener {
            startActivity(Intent(this, SignActivity::class.java))
        }
        binding.cardOcr.setOnClickListener {
            startActivity(Intent(this, OcrActivity::class.java))
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
