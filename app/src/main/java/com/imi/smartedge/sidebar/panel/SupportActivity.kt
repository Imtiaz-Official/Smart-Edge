package com.imi.smartedge.sidebar.panel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.snackbar.Snackbar
import com.imi.smartedge.sidebar.panel.databinding.ActivitySupportBinding

class SupportActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySupportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySupportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {


        binding.toolbar.title = "Support SidePanel"
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Setup Copy Listeners
        binding.btnCopyMobileBanking.setOnClickListener {
            copyToClipboard("Mobile Number", binding.tvMobileNumber.text.toString())
        }

        binding.btnCopyBinanceId.setOnClickListener {
            copyToClipboard("Binance ID", binding.tvBinanceId.text.toString())
        }

        binding.btnCopyTrc20.setOnClickListener {
            copyToClipboard("USDT (TRC20)", binding.tvTrc20.text.toString())
        }

        binding.btnCopyBep20.setOnClickListener {
            copyToClipboard("USDT (BEP20)", binding.tvBep20.text.toString())
        }

        binding.btnCopyErc20.setOnClickListener {
            copyToClipboard("USDT (ERC20)", binding.tvErc20.text.toString())
        }
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        
        binding.root.showModernToast("$label copied to clipboard!", Snackbar.LENGTH_SHORT)
    }
}
