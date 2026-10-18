package com.puthukan.bukutamu

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import java.text.SimpleDateFormat
import java.util.*
import java.text.NumberFormat
import android.os.Environment
import java.io.File
import java.io.FileOutputStream



class PengaturanActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pengaturan)

        // 1. Panggil fungsi dialog Kode Akses Database
        val menuKodeAkses = findViewById<android.view.View>(R.id.menuKodeAkses)
        menuKodeAkses.setOnClickListener {
            tampilkanDialogKodeAkses()
        }

        // 2. Panggil fungsi dialog tentang aplikai saat diklik
        val menuTentang = findViewById<android.view.View>(R.id.menuTentang)
        menuTentang.setOnClickListener {
            tampilkanDialogTentangAplikasi()
        }

        // 3. Panggil fungsi dialog Kelola Notifikasi
        val menuKelolaNotifikasi = findViewById<android.view.View>(R.id.menuNotifikasi)
        menuKelolaNotifikasi.setOnClickListener {
            tampilkanDialogAturAlarm()
        }

        // 4. Pengaturan Dark Mode
        val switchDarkMode = findViewById<SwitchCompat>(R.id.switchDarkMode)
        val sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)
        val isDarkModeActive = sharedPreferences.getBoolean("dark_mode", false)
        switchDarkMode.isChecked = isDarkModeActive

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPreferences.edit()
            editor.putBoolean("dark_mode", isChecked)
            editor.apply()

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // 5. Panggil fungsi dialog Rekapitulasi Total
        val menuRekapitulasi = findViewById<LinearLayout>(R.id.menuRekapitulasi)
        menuRekapitulasi.setOnClickListener {
            tampilkanDialogRekapitulasi()
        }

        // 6. Panggil fungsi dialog  BACKUP
        val menuUnduhData = findViewById<LinearLayout>(R.id.menuBackupRestore)
        menuUnduhData.setOnClickListener {
            // Memicu fungsi unduh saat tombol/menu diklik
            unduhDataKeFile()
        }

        // 7. Window Insets
        val rootView = findViewById<android.view.View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // 8. Bottom Navigation
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_pengaturan

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_beranda -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    finish()
                    true
                }
                R.id.nav_catatan -> {
                    startActivity(Intent(this, CatatanActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    finish()
                    true
                }
                R.id.nav_grafik -> {
                    startActivity(Intent(this, GrafikActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    finish()
                    true
                }
                R.id.nav_pengaturan -> true
                else -> false
            }
        }
    }

    private fun jadwalkanUlangAlarmSemuaTamu() {
        val context = this

        // 1. Ambil daftar tanggal acara yang tersimpan di lokal (Sesuaikan dengan cara Anda menyimpan data tamu/catatan)
        // Contoh jika Anda menyimpannya dalam bentuk JSON String di SharedPreferences atau database lokal:
        val sharedPrefs = getSharedPreferences("DbCatatanTamu", MODE_PRIVATE)
        val daftarTanggalJson = sharedPrefs.getString("list_tgl_acara", "[]") // Sesuaikan key penyimpanan data tamu Anda

        try {
            val jsonArray = org.json.JSONArray(daftarTanggalJson)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            for (i in 0 until jsonArray.length()) {
                val tanggalStr = jsonArray.getString(i)
                val triggerMillis = konversiTanggalKeMillisUntukSemua(context, tanggalStr)

                if (triggerMillis > System.currentTimeMillis()) {
                    val intent = Intent(context, AlarmReceiver::class.java).apply {
                        putExtra("pesan_notifikasi", "Pengingat Acara Tamu untuk tanggal $tanggalStr")
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        i, // Unique ID per alarm
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    // Pasang alarm tepat waktu
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Helper untuk konversi milidetik yang membaca SharedPreferences alarm_jam & alarm_menit terbaru
    private fun konversiTanggalKeMillisUntukSemua(context: Context, tanggalStr: String): Long {
        return try {
            val format = if (tanggalStr.contains("/")) {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            } else {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            }
            val date = format.parse(tanggalStr.substringBefore("T"))
            if (date != null) {
                val sharedPreferences = context.getSharedPreferences("DbConfig", MODE_PRIVATE)
                val alarmJam = sharedPreferences.getInt("alarm_jam", 7)     // Jam baru dari dialog
                val alarmMenit = sharedPreferences.getInt("alarm_menit", 0) // Menit baru dari dialog

                val calendar = Calendar.getInstance().apply {
                    time = date
                    set(Calendar.HOUR_OF_DAY, alarmJam)
                    set(Calendar.MINUTE, alarmMenit)
                    set(Calendar.SECOND, 0)
                }
                calendar.timeInMillis
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    private fun tampilkanDialogKodeAkses() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_kode_akses, null)

        val etLink = dialogView.findViewById<EditText>(R.id.etLinkDatabase)
        val etAuth = dialogView.findViewById<EditText>(R.id.etKodeAuth)
        val btnKembali = dialogView.findViewById<Button>(R.id.btnKembali)
        val btnHubungkan = dialogView.findViewById<Button>(R.id.btnAksi)

        btnHubungkan.text = "Hubungkan"

        val sharedPreferences = getSharedPreferences("DbConfig", MODE_PRIVATE)

        val savedLink = sharedPreferences.getString("db_link", "")
        val savedAuth = sharedPreferences.getString("db_auth", "")

        etLink.setText(savedLink)
        etAuth.setText(savedAuth)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        fun updateButtonState() {
            val linkText = etLink.text.toString().trim()
            val authText = etAuth.text.toString().trim()

            val isFilled = linkText.isNotEmpty() && authText.isNotEmpty()
            btnHubungkan.isEnabled = isFilled
            btnHubungkan.alpha = if (isFilled) 1.0f else 0.5f
        }

        updateButtonState()

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateButtonState()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        etLink.addTextChangedListener(textWatcher)
        etAuth.addTextChangedListener(textWatcher)

        btnKembali.setOnClickListener {
            dialog.dismiss()
        }

        btnHubungkan.setOnClickListener {
            val linkText = etLink.text.toString().trim()
            val authInput = etAuth.text.toString().trim()

            if (linkText.isEmpty() || authInput.isEmpty()) {
                Toast.makeText(this, "Link dan Kode Autentikator harus diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnHubungkan.isEnabled = false
            btnHubungkan.text = "Memeriksa..."

            Thread {
                var connection: HttpURLConnection? = null
                try {
                    val url = URL(linkText)
                    connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    connection.instanceFollowRedirects = true

                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val reader = BufferedReader(InputStreamReader(connection.inputStream))
                        val response = StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            response.append(line)
                        }
                        reader.close()

                        val jsonResponse = JSONObject(response.toString())
                        val serverAuthH1 = if (jsonResponse.has("h1")) jsonResponse.getString("h1").trim() else ""

                        runOnUiThread {
                            btnHubungkan.isEnabled = true
                            btnHubungkan.text = "Hubungkan"

                            if (authInput.equals(serverAuthH1, ignoreCase = true) && serverAuthH1.isNotEmpty()) {
                                val editor = sharedPreferences.edit()
                                editor.putString("db_link", linkText)
                                editor.putString("db_auth", authInput)
                                editor.apply()

                                Toast.makeText(this, "Koneksi Berhasil! Data disimpan.", Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                            } else {
                                Toast.makeText(this, "Gagal ! Autentikator: '$authInput' tidak sesuai", Toast.LENGTH_LONG).show()
                                etLink.text.clear()
                                etAuth.text.clear()
                            }
                        }
                    } else {
                        runOnUiThread {
                            btnHubungkan.isEnabled = true
                            btnHubungkan.text = "Hubungkan"
                            Toast.makeText(this, "HTTP Error Code: $responseCode", Toast.LENGTH_LONG).show()
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    val errorMsg = e.localizedMessage ?: "Unknown Error"
                    runOnUiThread {
                        btnHubungkan.isEnabled = true
                        btnHubungkan.text = "Hubungkan"
                        Toast.makeText(this, "Exception: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                } finally {
                    connection?.disconnect()
                }
            }.start()
        }

        dialog.show()
    }

    private fun tampilkanDialogAturAlarm() {
        val context = this
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 20)
            gravity = android.view.Gravity.CENTER
        }

        val tvLabel = TextView(context).apply {
            text = "Pilih Waktu Notifikasi Alarm"
            textSize = 15f
            setTextColor(resources.getColor(android.R.color.black, theme))
            setPadding(0, 0, 0, 20)
        }
        layout.addView(tvLabel)

        val layoutSpinner = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(20, 0, 20, 0)
        }

        // Ubah LayoutParams menggunakan weight agar lebarnya pas dan proporsional di dalam dialog
        val spinnerParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        // 1. Spinner Jam (00 - 23)
        val spinnerJam = Spinner(context).apply {
            layoutParams = spinnerParams
        }
        val listJam = Array(24) { String.format("%02d", it) }
        val adapterJam = ArrayAdapter(context, R.layout.spinner_item, listJam).apply {
            setDropDownViewResource(R.layout.spinner_item)
        }
        spinnerJam.adapter = adapterJam

        // Tanda Titik Dua (:)
        val tvTitikDua = TextView(context).apply {
            text = " : "
            textSize = 24f
            setTextColor(resources.getColor(android.R.color.black, theme))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        // 2. Spinner Menit (00 - 59)
        val spinnerMenit = Spinner(context).apply {
            layoutParams = spinnerParams
        }
        val listMenit = Array(60) { String.format("%02d", it) }
        val adapterMenit = ArrayAdapter(context, R.layout.spinner_item, listMenit).apply {
            setDropDownViewResource(R.layout.spinner_item)
        }
        spinnerMenit.adapter = adapterMenit

        // Masukkan ke layout horizontal
        layoutSpinner.addView(spinnerJam)
        layoutSpinner.addView(tvTitikDua)
        layoutSpinner.addView(spinnerMenit)

        layout.addView(layoutSpinner)

        // Ambil data jam & menit yang tersimpan sebelumnya di SharedPreferences
        val sharedPreferences = getSharedPreferences("DbConfig", MODE_PRIVATE)
        val savedJam = sharedPreferences.getInt("alarm_jam", 7)
        val savedMenit = sharedPreferences.getInt("alarm_menit", 0)

        spinnerJam.setSelection(savedJam)
        spinnerMenit.setSelection(savedMenit)

        // Tampilkan AlertDialog
        AlertDialog.Builder(context)
            .setTitle("Atur Jam Alarm")
            .setView(layout)
            .setPositiveButton("OK") { _, _ ->
                val selectedJam = spinnerJam.selectedItem.toString().toInt()
                val selectedMenit = spinnerMenit.selectedItem.toString().toInt()

                // 1. Simpan ke SharedPreferences
                sharedPreferences.edit().apply {
                    putInt("alarm_jam", selectedJam)
                    putInt("alarm_menit", selectedMenit)
                    apply()
                }

                // 2. JALANKAN PASANG ULANG ALARM
                jadwalkanUlangAlarmSemuaTamu()

                Toast.makeText(context, "Waktu alarm disimpan ($selectedJam:$selectedMenit)", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun tampilkanDialogRekapitulasi() {
        val sharedPrefs = getSharedPreferences("DbCatatanTamu", MODE_PRIVATE)
        // Membaca dari kunci cache_seluruh_tamu yang baru
        val dataJsonStr = sharedPrefs.getString("cache_seluruh_tamu", "[]")
            ?: sharedPrefs.getString("list_tgl_acara", "[]")
            ?: "[]"

        var totalTamu = 0
        var totalNominal = 0L

        try {
            val jsonArray = JSONArray(dataJsonStr)
            totalTamu = jsonArray.length() // Langsung hitung jumlah total item di array

            for (i in 0 until jsonArray.length()) {
                val itemStr = jsonArray.getString(i)
                if (itemStr.startsWith("{")) {
                    val obj = JSONObject(itemStr)
                    val nominalRaw = obj.optString("nominal_uang", "0")
                        .replace("[^0-9]".toRegex(), "")

                    if (nominalRaw.isNotEmpty()) {
                        totalNominal += nominalRaw.toLong()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Format mata uang Rupiah
        val localeID = Locale("in", "ID")
        val formatRupiah = NumberFormat.getCurrencyInstance(localeID)
        val nominalFormatted = formatRupiah.format(totalNominal)

        val pesanRekap = """
        Berikut adalah ringkasan seluruh data tamu yang tersimpan:

        • Total Tamu : $totalTamu Orang
        • Total Nominal Uang : $nominalFormatted
    """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Rekapitulasi Total")
            .setMessage(pesanRekap)
            .setIcon(android.R.drawable.ic_menu_sort_by_size)
            .setPositiveButton("Tutup") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun unduhDataKeFile() {
        val sharedPrefs = getSharedPreferences("DbCatatanTamu", MODE_PRIVATE)
        val dataJsonStr = sharedPrefs.getString("cache_seluruh_tamu", "[]") ?: "[]"

        try {
            val jsonArray = JSONArray(dataJsonStr)
            val csvBuilder = StringBuilder()

            // Header kolom file CSV
            csvBuilder.append("No ID,Nama,Alamat,Nominal Uang,Gawan,Tanggal Acara,Keterangan\n")

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val line = "${obj.optString("no_id")}," +
                        "\"${obj.optString("nama")}\"," +
                        "\"${obj.optString("alamat")}\"," +
                        "${obj.optString("nominal_uang")}," +
                        "\"${obj.optString("gawan")}\"," +
                        "${obj.optString("tgl_acara")}," +
                        "\"${obj.optString("keterangan")}\"\n"
                csvBuilder.append(line)
            }

            // Menyimpan file ke direktori Download publik di HP
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, "Laporan_BukuTamu_${System.currentTimeMillis()}.csv")

            FileOutputStream(file).use {
                it.write(csvBuilder.toString().toByteArray())
            }

            Toast.makeText(this, "Berhasil diunduh ke folder Download: ${file.name}", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal mengunduh data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun tampilkanDialogTentangAplikasi() {
        val pesanDeskripsi = """
        BT Mobile adalah aplikasi manajemen buku tamu dan acara digital yang dirancang untuk mempermudah pencatatan dan pemantauan agenda secara praktis.

        Fitur Utama:
        • Pencatatan & Sinkronisasi Google Sheets
        • Grafik Statistik Bulanan Dinamis
        • Pengingat Alarm & Notifikasi Otomatis
        • Mode Tampilan Gelap & Terang

        Versi Aplikasi: 1.0.0
        Dikembangkan oleh galeri Oemah_Puthukan.
    """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Tentang Aplikasi")
            .setMessage(pesanDeskripsi)
            .setIcon(android.R.drawable.ic_menu_info_details) // Ikon informasi bawaan Android
            .setPositiveButton("Tutup") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
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