package com.puthukan.bukutamu

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.concurrent.thread
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.ProgressBar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fabTambah: FloatingActionButton
    private lateinit var etSearch: EditText
    private lateinit var btnCalendar: ImageButton
    private lateinit var bottomNavigation: BottomNavigationView
    private var doubleBackToExitPressedOnce = false
    private lateinit var progressBar: ProgressBar

    private val listTamu = ArrayList<TamuModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Terapkan tema sebelum setContentView
        val sharedPreferencesTheme = getSharedPreferences("AppSettings", MODE_PRIVATE)
        val isDarkMode = sharedPreferencesTheme.getBoolean("dark_mode", false)

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressBar = findViewById(R.id.progressBar)

        // Mengatasi layar penuh / menabrak status bar
        val rootView = findViewById<android.view.View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // fungsi pengecekan izin Runtime Alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        recyclerView = findViewById(R.id.recyclerViewTamu)
        fabTambah = findViewById(R.id.fabTambah)
        etSearch = findViewById(R.id.etSearch)
        btnCalendar = findViewById(R.id.btnCalendar)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // Set default menu aktif
        bottomNavigation.selectedItemId = R.id.nav_beranda

        // Handle klik menu navigasi bawah
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_beranda -> true
                R.id.nav_catatan -> {
                    val intent = Intent(this, CatatanActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
                R.id.nav_grafik -> {
                    val intent = Intent(this, GrafikActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
                R.id.nav_pengaturan -> {
                    val intent = Intent(this, PengaturanActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
                else -> false
            }
        }

        // Ambil data dari spreadsheet saat aplikasi dibuka
        ambilDataDariSpreadsheet()

        fabTambah.setOnClickListener {
            tampilkanFormTambahTamu()
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterData(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnCalendar.setOnClickListener {
            tampilkanDatePicker()
        }
    }

    private fun getWebAppUrl(): String {
        val sharedPreferences = getSharedPreferences("DbConfig", MODE_PRIVATE)
        return sharedPreferences.getString("db_link", "") ?: ""
    }

    private fun ambilDataDariSpreadsheet() {
        val webAppUrl = getWebAppUrl()
        if (webAppUrl.isEmpty()) {
            runOnUiThread {
                Toast.makeText(this, "Link Database belum diatur di menu Pengaturan!", Toast.LENGTH_LONG).show()
            }
            return
        }

        // 1. TAMPILKAN LOADING
        runOnUiThread {
            progressBar.visibility = android.view.View.VISIBLE
        }

        thread {
            try {
                val url = URL(webAppUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val jsonResponse = JSONObject(response.toString())
                val jsonArray = jsonResponse.getJSONArray("tamu")

                listTamu.clear()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)

                    val rawTgl = obj.getString("tgl_acara")
                    val cleanTgl = when {
                        rawTgl.contains("GMT") -> {
                            try {
                                val parts = rawTgl.split(" ")
                                val day = parts[2]
                                val monthStr = parts[1]
                                val year = parts[3]
                                val monthMap = mapOf(
                                    "Jan" to "01", "Feb" to "02", "Mar" to "03", "Apr" to "04",
                                    "May" to "05", "Jun" to "06", "Jul" to "07", "Aug" to "08",
                                    "Sep" to "09", "Oct" to "10", "Nov" to "11", "Dec" to "12"
                                )
                                val month = monthMap[monthStr] ?: "01"
                                "$year-$month-$day"
                            } catch (e: Exception) {
                                rawTgl.substringBefore("GMT").trim()
                            }
                        }
                        rawTgl.contains("T") -> rawTgl.substringBefore("T")
                        else -> rawTgl
                    }

                    val tamu = TamuModel(
                        no_id = obj.getString("no_id"),
                        nama = obj.getString("nama"),
                        alamat = obj.getString("alamat"),
                        nominal_uang = obj.getString("nominal_uang"),
                        gawan = obj.getString("gawan"),
                        tgl_acara = cleanTgl,
                        keterangan = obj.getString("keterangan")
                    )
                    listTamu.add(tamu)
                }

                // Simpan daftar tanggal acara ke SharedPreferences agar bisa dibaca oleh GrafikActivity
                try {
                    val jsonArrayTamu = JSONArray()
                    for (tamu in listTamu) {
                        if (tamu.tgl_acara.isNotEmpty()) {
                            val objTamu = JSONObject().apply {
                                put("no_id", tamu.no_id)
                                put("nama", tamu.nama)
                                put("alamat", tamu.alamat)
                                put("nominal_uang", tamu.nominal_uang)
                                put("gawan", tamu.gawan)
                                put("tgl_acara", tamu.tgl_acara)
                                put("keterangan", tamu.keterangan)
                            }
                            jsonArrayTamu.put(objTamu)
                        }
                    }
                    val sharedPrefs = getSharedPreferences("DbCatatanTamu", MODE_PRIVATE)
                    sharedPrefs.edit().putString("list_tgl_acara", jsonArrayTamu.toString()).apply()

                    simpanCacheTamuLengkap(listTamu)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Simpan cache seluruh tamu lengkap untuk rekapitulasi total di menu Pengaturan
                runOnUiThread {
                    tampilkanKeAdapter(listTamu)
                    try {
                        jadwalkanNotifikasiTamu(listTamu)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Gagal memuat data dari server", Toast.LENGTH_SHORT).show()
                }
            } finally {
                // 2. KUNCI UTAMA: HILANGKAN LOADING DI BLOK FINALLY (PASTI DIEKSEKUSI)
                runOnUiThread {
                    progressBar.visibility = android.view.View.GONE
                }
            }
        }
    }

    private fun simpanCacheTamuLengkap(listTamu: List<TamuModel>) {
        val jsonArray = JSONArray()
        for (tamu in listTamu) {
            val obj = JSONObject().apply {
                put("no_id", tamu.no_id)
                put("nama", tamu.nama)
                put("alamat", tamu.alamat)
                put("nominal_uang", tamu.nominal_uang)
                put("gawan", tamu.gawan)
                put("tgl_acara", tamu.tgl_acara)
                put("keterangan", tamu.keterangan)
            }
            jsonArray.put(obj)
        }

        val sharedPrefs = getSharedPreferences("DbCatatanTamu", MODE_PRIVATE)
        sharedPrefs.edit()
            .putString("cache_seluruh_tamu", jsonArray.toString())
            .apply()
    }

    private fun jadwalkanNotifikasiTamu(list: List<TamuModel>) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val currentTime = System.currentTimeMillis()

        for (tamu in list) {
            if (tamu.tgl_acara.isNotEmpty()) {
                try {
                    val targetMillis = konversiTanggalKeMillis(tamu.tgl_acara)

                    val intent = Intent(this, AlarmReceiver::class.java).apply {
                        putExtra("EXTRA_NAMA", tamu.nama)
                        putExtra("EXTRA_TANGGAL", tamu.tgl_acara)
                    }

                    val requestCode = try {
                        tamu.no_id.hashCode()
                    } catch (e: Exception) {
                        tamu.hashCode()
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        this,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    // PENTING: Batalkan dulu alarm lama dengan requestCode yang sama agar tidak menumpuk
                    alarmManager.cancel(pendingIntent)

                    // KUNCI UTAMA: Hanya jadwalkan jika waktu target BERADA DI MASA DEPAN (> currentTime)
                    if (targetMillis > currentTime) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (alarmManager.canScheduleExactAlarms()) {
                                alarmManager.setExactAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    targetMillis,
                                    pendingIntent
                                )
                            } else {
                                alarmManager.set(
                                    AlarmManager.RTC_WAKEUP,
                                    targetMillis,
                                    pendingIntent
                                )
                            }
                        } else {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                targetMillis,
                                pendingIntent
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun konversiTanggalKeMillis(tanggalStr: String): Long {
        return try {
            val cleanDateStr = tanggalStr.trim().substringBefore("T").substringBefore(" ")

            val format = if (cleanDateStr.contains("/")) {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            } else {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            }

            val date = format.parse(cleanDateStr)
            if (date != null) {
                val sharedPreferences = getSharedPreferences("DbConfig", MODE_PRIVATE)
                val alarmJam = sharedPreferences.getInt("alarm_jam", 7)
                val alarmMenit = sharedPreferences.getInt("alarm_menit", 0)

                val calendar = Calendar.getInstance().apply {
                    time = date
                    set(Calendar.HOUR_OF_DAY, alarmJam)
                    set(Calendar.MINUTE, alarmMenit)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                calendar.timeInMillis
            } else {
                0L
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    private fun filterData(keyword: String) {
        val filteredList = ArrayList<TamuModel>()
        val cleanKeyword = keyword.trim()

        for (tamu in listTamu) {
            val cleanTglAcara = normalisasiTanggal(tamu.tgl_acara)
            if (tamu.nama.lowercase().contains(cleanKeyword.lowercase()) || cleanTglAcara.contains(cleanKeyword)) {
                filteredList.add(tamu)
            }
        }
        tampilkanKeAdapter(filteredList)
    }

    private fun normalisasiTanggal(tanggalStr: String): String {
        return try {
            when {
                tanggalStr.contains("T") -> {
                    val datePart = tanggalStr.substringBefore("T")
                    val parts = datePart.split("-")
                    if (parts.size >= 3) {
                        "${parts[2]}/${parts[1]}/${parts[0]}"
                    } else {
                        tanggalStr
                    }
                }
                tanggalStr.length >= 10 && tanggalStr.contains("-") && !tanggalStr.contains("/") -> {
                    val parts = tanggalStr.substring(0, 10).split("-")
                    if (parts.size >= 3) {
                        "${parts[2]}/${parts[1]}/${parts[0]}"
                    } else {
                        tanggalStr
                    }
                }
                tanggalStr.length >= 10 && tanggalStr.contains("/") -> {
                    tanggalStr.substring(0, 10)
                }
                else -> tanggalStr
            }
        } catch (e: Exception) {
            tanggalStr
        }
    }

    private fun tampilkanKeAdapter(data: List<TamuModel>) {
        val adapter = TamuAdapter(data) { tamu ->
            val pilihan = arrayOf("Edit Data", "Hapus Data")
            AlertDialog.Builder(this)
                .setTitle("Pilih Aksi (${tamu.nama})")
                .setItems(pilihan) { _, which ->
                    when (which) {
                        0 -> tampilkanFormEditTamu(tamu)
                        1 -> konfirmasiHapusData(tamu.no_id)
                    }
                }
                .show()
        }
        recyclerView.adapter = adapter
    }

    private fun tampilkanDatePicker() {
        val calendar = Calendar.getInstance()
        val tahun = calendar.get(Calendar.YEAR)
        val bulan = calendar.get(Calendar.MONTH)
        val hari = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val bulanFormatted = String.format("%02d", selectedMonth + 1)
            val hariFormatted = String.format("%02d", selectedDay)
            val tanggalPilihan = "$hariFormatted/$bulanFormatted/$selectedYear"

            etSearch.setText(tanggalPilihan)
        }, tahun, bulan, hari).show()
    }

    private fun tampilkanFormTambahTamu() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 30, 40, 10)
        }

        val etNama = EditText(this).apply { hint = "Nama Tamu" }
        val etAlamat = EditText(this).apply { hint = "Alamat / Dusun" }
        val etNominal = EditText(this).apply { hint = "Nominal Uang (Contoh: 50000)" }
        val etGawan = EditText(this).apply { hint = "Bawaan / Gawan (Opsional)" }
        val etTanggal = EditText(this).apply { hint = "Tanggal Acara (Contoh: 12/10/2026)" }
        val etKeterangan = EditText(this).apply { hint = "Keterangan (Opsional)" }

        layout.addView(etNama)
        layout.addView(etAlamat)
        layout.addView(etNominal)
        layout.addView(etGawan)
        layout.addView(etTanggal)
        layout.addView(etKeterangan)

        AlertDialog.Builder(this)
            .setTitle("Tambah Tamu")
            .setView(layout)
            .setPositiveButton("Simpan") { _, _ ->
                val nama = etNama.text.toString()
                val alamat = etAlamat.text.toString()
                val nominal = etNominal.text.toString()
                val gawan = etGawan.text.toString()
                val tanggal = etTanggal.text.toString()
                val keterangan = etKeterangan.text.toString()

                kirimAksiKeSpreadsheet("insert", "", nama, alamat, nominal, gawan, tanggal, keterangan)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun tampilkanFormEditTamu(tamu: TamuModel) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 30, 40, 10)
        }

        val etNama = EditText(this).apply {
            hint = "Nama Tamu"
            setText(tamu.nama)
        }
        val etAlamat = EditText(this).apply {
            hint = "Alamat / Dusun"
            setText(tamu.alamat)
        }
        val etNominal = EditText(this).apply {
            hint = "Nominal Uang (Contoh: 50000)"
            setText(tamu.nominal_uang)
        }
        val etGawan = EditText(this).apply {
            hint = "Bawaan / Gawan (Opsional)"
            setText(tamu.gawan)
        }
        val etTanggal = EditText(this).apply {
            hint = "Tanggal Acara (Contoh: 12/10/2026)"
            setText(tamu.tgl_acara)
        }
        val etKeterangan = EditText(this).apply {
            hint = "Keterangan (Opsional)"
            setText(tamu.keterangan)
        }

        layout.addView(etNama)
        layout.addView(etAlamat)
        layout.addView(etNominal)
        layout.addView(etGawan)
        layout.addView(etTanggal)
        layout.addView(etKeterangan)

        AlertDialog.Builder(this)
            .setTitle("Edit Buku Tamu")
            .setView(layout)
            .setPositiveButton("Update") { _, _ ->
                kirimAksiKeSpreadsheet(
                    action = "update",
                    noId = tamu.no_id,
                    nama = etNama.text.toString(),
                    alamat = etAlamat.text.toString(),
                    nominal = etNominal.text.toString(),
                    gawan = etGawan.text.toString(),
                    tanggal = etTanggal.text.toString(),
                    ket = etKeterangan.text.toString()
                )
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun konfirmasiHapusData(noId: String) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Data")
            .setMessage("Apakah Anda yakin ingin menghapus data tamu ini?")
            .setPositiveButton("Hapus") { _, _ ->
                kirimAksiKeSpreadsheet("delete", noId, "", "", "", "", "", "")
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun kirimAksiKeSpreadsheet(action: String, noId: String, nama: String, alamat: String, nominal: String, gawan: String, tanggal: String, ket: String) {
        val webAppUrl = getWebAppUrl()
        if (webAppUrl.isEmpty()) {
            Toast.makeText(this, "Link Database belum diatur di menu Pengaturan!", Toast.LENGTH_LONG).show()
            return
        }

        thread {
            try {
                val url = URL(webAppUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true

                val postData = "action=$action" +
                        "&no_id=${URLEncoder.encode(noId, "UTF-8")}" +
                        "&nama=${URLEncoder.encode(nama, "UTF-8")}" +
                        "&alamat=${URLEncoder.encode(alamat, "UTF-8")}" +
                        "&nominal_uang=${URLEncoder.encode(nominal, "UTF-8")}" +
                        "&gawan=${URLEncoder.encode(gawan, "UTF-8")}" +
                        "&tgl_acara=${URLEncoder.encode(tanggal, "UTF-8")}" +
                        "&keterangan=${URLEncoder.encode(ket, "UTF-8")}"

                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(postData)
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    runOnUiThread {
                        Toast.makeText(this, "Berhasil memproses data!", Toast.LENGTH_SHORT).show()
                        ambilDataDariSpreadsheet()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Gagal menghubungi server", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }

        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Tekan sekali lagi untuk keluar", Toast.LENGTH_SHORT).show()

        android.os.Handler(mainLooper).postDelayed({
            doubleBackToExitPressedOnce = false
        }, 2000)
    }
}