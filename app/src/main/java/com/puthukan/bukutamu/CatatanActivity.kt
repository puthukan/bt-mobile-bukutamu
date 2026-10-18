package com.puthukan.bukutamu // Sesuaikan package Anda

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class CatatanActivity : AppCompatActivity() {

    private lateinit var etNotepad: EditText
    private lateinit var btnSimpan: Button
    private lateinit var btnHapus: Button
    private lateinit var bottomNavigation: BottomNavigationView

    private val PREF_NAME = "PrefCatatanPribadi"
    private val KEY_ISI_CATATAN = "isi_catatan"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catatan)

        // Mengatur insets agar tidak menabrak status bar
        val rootView = findViewById<android.view.View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // systemBars.bottom JANGAN ditambahkan ke padding root view agar layout bisa turun mentok ke bawah
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        etNotepad = findViewById(R.id.etNotepad)
        btnSimpan = findViewById(R.id.btnSimpanCatatan)
        btnHapus = findViewById(R.id.btnHapusCatatan)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        // Set menu aktif ke Catatan
        bottomNavigation.selectedItemId = R.id.nav_catatan

        // 1. Muat catatan yang sebelumnya tersimpan saat halaman dibuka
        muatCatatan()

        // 2. Tombol Simpan diklik
        btnSimpan.setOnClickListener {
            simpanCatatan()
        }

        // 3. Tombol Hapus diklik
        btnHapus.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Hapus Catatan")
                .setMessage("Apakah Anda yakin ingin menghapus seluruh catatan ini?")
                .setPositiveButton("Hapus") { _, _ ->
                    etNotepad.setText("")
                    simpanCatatan()
                    Toast.makeText(this, "Catatan berhasil dihapus", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        // 4. Navigasi Bawah dengan Animasi Geser
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_beranda -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    finish()
                    true
                }
                R.id.nav_catatan -> true
                R.id.nav_grafik -> {
                    startActivity(Intent(this, GrafikActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
                R.id.nav_pengaturan -> {
                    startActivity(Intent(this, PengaturanActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun simpanCatatan() {
        val isiTeks = etNotepad.text.toString()
        val sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(KEY_ISI_CATATAN, isiTeks)
        editor.apply()
        Toast.makeText(this, "Catatan tersimpan", Toast.LENGTH_SHORT).show()
    }

    private fun muatCatatan() {
        val sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val savedText = sharedPreferences.getString(KEY_ISI_CATATAN, "")
        etNotepad.setText(savedText)
    }

    // KETIKA DI TEKAN KEMBALI AKAN MENGARAH KE HALAMAN UTAMA (Mainactifity)
    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        // FLAG agar tidak membuat tumpukan baru jika MainActivity sudah ada
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        finish()
    }
}