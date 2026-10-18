package com.puthukan.bukutamu // Sesuaikan package Anda

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class TamuAdapter(
    private val listTamu: List<TamuModel>,
    private val onItemClick: (TamuModel) -> Unit
) : RecyclerView.Adapter<TamuAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNama: TextView = view.findViewById(R.id.tvNama)
        val tvTanggal: TextView = view.findViewById(R.id.tvTanggal)
        val tvAlamat: TextView = view.findViewById(R.id.tvAlamat)
        val tvGawan: TextView = view.findViewById(R.id.tvGawan)
        val tvKeterangan: TextView = view.findViewById(R.id.tvKeterangan)
        val tvNominal: TextView = view.findViewById(R.id.tvNominal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tamu, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tamu = listTamu[position]
        holder.tvNama.text = tamu.nama
        holder.tvAlamat.text = tamu.alamat
        holder.tvTanggal.text = formatTanggal(tamu.tgl_acara)
        holder.tvNominal.text = formatRupiah(tamu.nominal_uang)

        // Menampilkan Gawan (Kolom E) di bawah nominal uang jika ada isinya
        if (!tamu.gawan.isNullOrEmpty() && tamu.gawan != "null" && tamu.gawan != "-") {
            holder.tvGawan.visibility = View.VISIBLE
            holder.tvGawan.text = "Gawan: ${tamu.gawan}"
        } else {
            holder.tvGawan.visibility = View.GONE // Otomatis hilang jika kosong
        }

        // Menampilkan Keterangan (Kolom G) di bawah alamat jika ada isinya
        if (!tamu.keterangan.isNullOrEmpty() && tamu.keterangan != "null") {
            holder.tvKeterangan.visibility = View.VISIBLE
            holder.tvKeterangan.text = tamu.keterangan
        } else {
            holder.tvKeterangan.visibility = View.GONE // Otomatis hilang jika kosong
        }

        // Aksi klik kartu untuk Edit / Hapus
        holder.itemView.setOnClickListener {
            onItemClick(tamu)
        }
    }

    override fun getItemCount(): Int = listTamu.size

    private fun formatTanggal(tanggalStr: String): String {
        return try {
            if (tanggalStr.contains("T")) {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val date = inputFormat.parse(tanggalStr)
                if (date != null) outputFormat.format(date) else tanggalStr
            } else {
                tanggalStr
            }
        } catch (e: Exception) {
            tanggalStr
        }
    }

    private fun formatRupiah(nominalStr: String): String {
        return try {
            val angka = nominalStr.replace("[^\\d]".toRegex(), "").toDoubleOrNull() ?: 0.0
            val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            formatter.maximumFractionDigits = 0
            formatter.format(angka)
        } catch (e: Exception) {
            "Rp $nominalStr"
        }
    }
}