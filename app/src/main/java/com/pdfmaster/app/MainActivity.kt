package com.pdfmaster.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.pdfmaster.app.databinding.ActivityMainBinding
import com.pdfmaster.app.tools.ToolsActivity
import com.pdfmaster.app.viewer.ViewerActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val pickPdf = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val intent = Intent(this, ViewerActivity::class.java)
            intent.putExtra("pdf_uri", uri.toString())
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cardViewPdf.setOnClickListener {
            pickPdf.launch("application/pdf")
        }

        binding.cardTools.setOnClickListener {
            startActivity(Intent(this, ToolsActivity::class.java))
        }
    }
}
