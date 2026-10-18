package com.puthukan.bukutamu

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

class GrafikActivity : AppCompatActivity() {

    private lateinit var spinnerBulan: Spinner
    private lateinit var spinnerTahun: Spinner
    private lateinit var lineChart: LineChart
    private lateinit var recyclerViewTamuGrafik: RecyclerView
    private lateinit var tvJudulDaftarTamu: TextView

    private val daftarBulan = arrayOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grafik)

        spinnerBulan = findViewById(R.id.spinnerBulan)
        spinnerTahun = findViewById(R.id.spinnerTahun)
        lineChart = findViewById(R.id.lineChart)
        recyclerViewTamuGrafik = findViewById(R.id.recyclerViewTamuGrafik)
        tvJudulDaftarTamu = findViewById(R.id.tvJudulDaftarTamu)

        recyclerViewTamuGrafik.layoutManager = LinearLayoutManager(this)

        // Tambahkan di dalam onCreate() setelah setContentView(R.layout.activity_grafik)
        val rootView = findViewById<android.view.View>(android.R.id.content)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // Bottom Navigation
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_grafik

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
                R.id.nav_grafik -> true
                R.id.nav_pengaturan -> {
                    startActivity(Intent(this, PengaturanActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
                else -> false
            }
        }

        setupSpinners()
    }

    private fun setupSpinners() {
        val adapterBulan = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, daftarBulan)
        spinnerBulan.adapter = adapterBulan

        val calendarNow = Calendar.getInstance()
        val tahunSekarang = calendarNow.get(Calendar.YEAR)
        val listTahun = ArrayList<String>()
        for (i in (tahunSekarang - 3)..(tahunSekarang + 3)) {
            listTahun.add(i.toString())
        }

        val adapterTahun = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listTahun)
        spinnerTahun.adapter = adapterTahun

        val bulanSekarang = calendarNow.get(Calendar.MONTH)
        spinnerBulan.setSelection(bulanSekarang)
        val posTahun = listTahun.indexOf(tahunSekarang.toString())
        if (posTahun >= 0) spinnerTahun.setSelection(posTahun)

        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateGrafikDanDaftarTamu()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerBulan.onItemSelectedListener = listener
        spinnerTahun.onItemSelectedListener = listener

        updateGrafikDanDaftarTamu()
    }

    private fun updateGrafikDanDaftarTamu() {
        val bulanIndex = spinnerBulan.selectedItemPosition
        val namaBulanStr = daftarBulan[bulanIndex]
        val tahunSelected = spinnerTahun.selectedItem.toString().toInt()

        tvJudulDaftarTamu.text = "Daftar Tamu Bulan $namaBulanStr $tahunSelected:"

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, tahunSelected)
        calendar.set(Calendar.MONTH, bulanIndex)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val jumlahHariDalamBulan = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val rekapAcaraPerHari = IntArray(jumlahHariDalamBulan + 1) { 0 }
        val listTamuBulanIni = ArrayList<TamuModel>()

        // Mengambil data dari SharedPreferences "DbCatatanTamu"
        val sharedPrefs = getSharedPreferences("DbCatatanTamu", MODE_PRIVATE)
        val dataJsonStr = sharedPrefs.getString("list_tgl_acara", "[]") ?: "[]"

        // Catatan: Jika Anda ingin menampilkan detail nama tamu, alamat, dll.,
        // pastikan MainActivity juga menyimpan array objek JSON tamu lengkap,
        // atau kita filter dari list lokal jika ada penyimpanannya.
        // Di bawah ini kita parsing tanggal dan mencocokkannya:
        try {
            val jsonArray = JSONArray(dataJsonStr)
            for (i in 0 until jsonArray.length()) {
                val itemStr = jsonArray.getString(i)

                // Mendukung format string tanggal biasa atau objek tamu jika disimpan bentuk JSON
                val tglAcaraStr = if (itemStr.startsWith("{")) {
                    val obj = org.json.JSONObject(itemStr)
                    val rawTgl = obj.optString("tgl_acara", "")

                    // Masukkan juga objeknya ke list jika sesuai bulan/tahun
                    val dateObj = parseTanggal(rawTgl)
                    if (dateObj != null) {
                        val calData = Calendar.getInstance()
                        calData.time = dateObj
                        if (calData.get(Calendar.YEAR) == tahunSelected && calData.get(Calendar.MONTH) == bulanIndex) {
                            listTamuBulanIni.add(
                                TamuModel(
                                    no_id = obj.optString("no_id"),
                                    nama = obj.optString("nama"),
                                    alamat = obj.optString("alamat"),
                                    nominal_uang = obj.optString("nominal_uang"),
                                    gawan = obj.optString("gawan"),
                                    tgl_acara = rawTgl,
                                    keterangan = obj.optString("keterangan")
                                )
                            )
                        }
                    }
                    rawTgl
                } else {
                    itemStr
                }

                val dateObj = parseTanggal(tglAcaraStr)
                if (dateObj != null) {
                    val calData = Calendar.getInstance()
                    calData.time = dateObj

                    if (calData.get(Calendar.YEAR) == tahunSelected && calData.get(Calendar.MONTH) == bulanIndex) {
                        val tglData = calData.get(Calendar.DAY_OF_MONTH)
                        if (tglData in 1..jumlahHariDalamBulan) {
                            rekapAcaraPerHari[tglData]++
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Tampilkan ke RecyclerView Daftar Tamu
        val adapterTamu = TamuAdapter(listTamuBulanIni) { tamu ->
            // Aksi klik item tamu di grafik jika diperlukan (opsional)
        }
        recyclerViewTamuGrafik.adapter = adapterTamu

        // Setup Data Grafik
        val entries = ArrayList<Entry>()
        for (hari in 1..jumlahHariDalamBulan) {
            entries.add(Entry(hari.toFloat(), rekapAcaraPerHari[hari].toFloat()))
        }

        val dataSet = LineDataSet(entries, "Jumlah Acara").apply {
            color = Color.parseColor("#673AB7")
            setCircleColor(Color.parseColor("#673AB7"))
            circleRadius = 4f
            circleHoleRadius = 2f
            lineWidth = 2f
            valueTextSize = 9f
            setDrawValues(true)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value > 0f) value.toInt().toString() else ""
                }
            }
        }

        val lineData = LineData(dataSet)
        lineChart.data = lineData

        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)

        val yAxisLeft = lineChart.axisLeft
        yAxisLeft.axisMinimum = 0f
        yAxisLeft.axisMaximum = 15f
        yAxisLeft.granularity = 1f

        lineChart.axisRight.isEnabled = false
        lineChart.description.isEnabled = false
        lineChart.animateX(500)
        lineChart.invalidate()
    }

    private fun parseTanggal(tanggalStr: String): Date? {
        return try {
            val cleanStr = tanggalStr.trim().substringBefore("T").substringBefore(" ")
            val format = if (cleanStr.contains("/")) {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            } else {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            }
            format.parse(cleanStr)
        } catch (e: Exception) {
            null
        }
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