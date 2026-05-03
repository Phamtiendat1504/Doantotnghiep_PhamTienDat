package com.example.doantotnghiep.View.Auth

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.doantotnghiep.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.button.MaterialButton
import java.util.Locale
import java.util.concurrent.Executors

class LocationPickerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_INITIAL_ADDRESS  = "initial_address"
        const val EXTRA_INITIAL_WARD     = "initial_ward"
        const val EXTRA_INITIAL_DISTRICT = "initial_district"
        const val EXTRA_RESULT_ADDRESS   = "result_address"
        const val EXTRA_RESULT_LAT       = "result_lat"
        const val EXTRA_RESULT_LNG       = "result_lng"
    }

    private lateinit var btnBack: ImageView
    private lateinit var btnSearchPlace: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnConfirm: MaterialButton
    private lateinit var tvPickedAddress: TextView

    private var googleMap: GoogleMap? = null
    private var selectedLatLng: LatLng? = null
    private var selectedAddress: String = ""
    private val hanoiLatLng = LatLng(21.0285, 105.8542)
    private val geocodeExecutor = Executors.newSingleThreadExecutor()

    // ────────────────────────────────────────────────
    // Lifecycle
    // ────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_picker)

        btnBack         = findViewById(R.id.btnBack)
        btnSearchPlace  = findViewById(R.id.btnSearchPlace)
        btnCancel       = findViewById(R.id.btnCancel)
        btnConfirm      = findViewById(R.id.btnConfirm)
        tvPickedAddress = findViewById(R.id.tvPickedAddress)

        setupMap()
        setupActions()
        applyInitialAddress()
    }

    override fun onDestroy() {
        super.onDestroy()
        geocodeExecutor.shutdownNow()
    }

    // ────────────────────────────────────────────────
    // Map
    // ────────────────────────────────────────────────
    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapPickerContainer) as? SupportMapFragment ?: return
        mapFragment.getMapAsync { map ->
            googleMap = map
            map.uiSettings.isZoomControlsEnabled = true
            map.uiSettings.isCompassEnabled      = true
            map.uiSettings.isMapToolbarEnabled   = true
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(hanoiLatLng, 11f))
            map.setOnMapClickListener { latLng ->
                selectedLatLng = latLng
                updateMarker(latLng, true)
                reverseGeocode(latLng)
            }
        }
    }

    // ────────────────────────────────────────────────
    // Button listeners
    // ────────────────────────────────────────────────
    private fun setupActions() {
        btnBack.setOnClickListener       { finish() }
        btnCancel.setOnClickListener     { finish() }
        btnSearchPlace.setOnClickListener { openSearchDialog() }
        btnConfirm.setOnClickListener    { confirmSelection() }
    }

    // ────────────────────────────────────────────────
    // Custom search dialog (Geocoder - khong can Places API)
    // ────────────────────────────────────────────────
    private fun openSearchDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_search_address, null)
        val edtSearch = dialogView.findViewById<EditText>(R.id.edtSearchQuery)

        val dialog = AlertDialog.Builder(this, R.style.RoundedDialogStyle)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDialogCancel)
            .setOnClickListener { dialog.dismiss() }

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDialogSearch)
            .setOnClickListener {
                val query = edtSearch.text.toString().trim()
                if (query.isNotEmpty()) {
                    dialog.dismiss()
                    geocodeAndShow(query)
                } else {
                    edtSearch.error = "Vui lòng nhập địa chỉ"
                }
            }

        edtSearch.setOnEditorActionListener { _, _, _ ->
            val query = edtSearch.text.toString().trim()
            if (query.isNotEmpty()) { dialog.dismiss(); geocodeAndShow(query) }
            true
        }

        dialog.show()
        edtSearch.requestFocus()
    }

    @Suppress("DEPRECATION")
    private fun geocodeAndShow(query: String) {
        val fullQuery = if (query.contains("Ha Noi", ignoreCase = true) ||
            query.contains("Hanoi", ignoreCase = true) ||
            query.contains("Ha N\u1ed9i", ignoreCase = true))
            query else "$query, H\u00e0 N\u1ed9i, Vi\u1ec7t Nam"

        geocodeExecutor.execute {
            val geocoder = Geocoder(this, Locale("vi", "VN"))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                runCatching {
                    geocoder.getFromLocationName(fullQuery, 5) { results ->
                        handleGeocodeResults(results)
                    }
                }.onFailure {
                    handleGeocodeResults(emptyList())
                }
                return@execute
            } else {
                val results = try {
                    geocoder.getFromLocationName(fullQuery, 5) ?: emptyList()
                } catch (_: Exception) { emptyList() }
                handleGeocodeResults(results)
            }
        }
    }

    private fun handleGeocodeResults(results: List<android.location.Address>) {
        if (isFinishing || isDestroyed) return

        runOnUiThread {
            when {
                results.isEmpty() -> Toast.makeText(
                    this, "Kh\u00f4ng t\u00ecm th\u1ea5y. H\u00e3y th\u1eed t\u1eeb kh\u00f3a kh\u00e1c.",
                    Toast.LENGTH_SHORT
                ).show()
                results.size == 1 -> applyGeocoderResult(results.first())
                else              -> showResultPicker(results)
            }
        }
    }

    private fun showResultPicker(results: List<android.location.Address>) {
        val labels = results.map { addr ->
            (0..addr.maxAddressLineIndex).joinToString(", ") { addr.getAddressLine(it) }
        }
        AlertDialog.Builder(this)
            .setTitle("Ch\u1ecdn \u0111\u1ecba ch\u1ec9")
            .setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, labels)) { _, idx ->
                applyGeocoderResult(results[idx])
            }
            .setNegativeButton("H\u1ee7y", null)
            .show()
    }

    private fun applyGeocoderResult(addr: android.location.Address) {
        val latLng = LatLng(addr.latitude, addr.longitude)
        selectedLatLng  = latLng
        selectedAddress = (0..addr.maxAddressLineIndex)
            .joinToString(", ") { addr.getAddressLine(it) }
        renderPickedAddress()
        updateMarker(latLng, true)
    }

    // ────────────────────────────────────────────────
    // Reverse geocode khi tap tren ban do
    // ────────────────────────────────────────────────
    @Suppress("DEPRECATION")
    private fun reverseGeocode(latLng: LatLng) {
        geocodeExecutor.execute {
            val geocoder = Geocoder(this, Locale("vi", "VN"))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) { results ->
                    if (!isFinishing && !isDestroyed) runOnUiThread {
                        selectedAddress = results.firstOrNull()?.getAddressLine(0)
                            ?: "${latLng.latitude}, ${latLng.longitude}"
                        renderPickedAddress()
                    }
                }
            } else {
                val result = try {
                    geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                } catch (_: Exception) { null }
                if (!isFinishing && !isDestroyed) runOnUiThread {
                    selectedAddress = result?.firstOrNull()?.getAddressLine(0)
                        ?: "${latLng.latitude}, ${latLng.longitude}"
                    renderPickedAddress()
                }
            }
        }
    }

    // ────────────────────────────────────────────────
    // Khoi tao marker theo dia chi ban dau
    // ────────────────────────────────────────────────
    @Suppress("DEPRECATION")
    private fun applyInitialAddress() {
        val initial  = intent.getStringExtra(EXTRA_INITIAL_ADDRESS).orEmpty().trim()
        val ward     = intent.getStringExtra(EXTRA_INITIAL_WARD).orEmpty().trim()
        val district = intent.getStringExtra(EXTRA_INITIAL_DISTRICT).orEmpty().trim()
        val full = buildString {
            if (initial.isNotEmpty())  append(initial).append(", ")
            if (ward.isNotEmpty())     append(ward).append(", ")
            if (district.isNotEmpty()) append(district).append(", ")
            append("H\u00e0 N\u1ed9i")
        }
        if (full.isBlank()) return

        geocodeExecutor.execute {
            val geocoder = Geocoder(this, Locale("vi", "VN"))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocationName(full, 1) { results ->
                    results.firstOrNull()?.let { addr ->
                        val latLng = LatLng(addr.latitude, addr.longitude)
                        if (!isFinishing && !isDestroyed) runOnUiThread {
                            selectedLatLng  = latLng
                            selectedAddress = addr.getAddressLine(0) ?: full
                            renderPickedAddress()
                            updateMarker(latLng, true)
                        }
                    }
                }
            } else {
                val result = try {
                    geocoder.getFromLocationName(full, 1)
                } catch (_: Exception) { null }
                result?.firstOrNull()?.let { addr ->
                    val latLng = LatLng(addr.latitude, addr.longitude)
                    if (!isFinishing && !isDestroyed) runOnUiThread {
                        selectedLatLng  = latLng
                        selectedAddress = addr.getAddressLine(0) ?: full
                        renderPickedAddress()
                        updateMarker(latLng, true)
                    }
                }
            }
        }
    }

    // ────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────
    private fun updateMarker(latLng: LatLng, animate: Boolean) {
        val map = googleMap ?: return
        map.clear()
        map.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(if (selectedAddress.isNotBlank()) selectedAddress else "V\u1ecb tr\u00ed \u0111\u00e3 ch\u1ecdn")
        )
        val update = CameraUpdateFactory.newLatLngZoom(latLng, 16f)
        if (animate) map.animateCamera(update) else map.moveCamera(update)
    }

    private fun renderPickedAddress() {
        tvPickedAddress.text =
            if (selectedAddress.isNotBlank()) selectedAddress else "Ch\u01b0a ch\u1ecdn v\u1ecb tr\u00ed"
    }

    private fun confirmSelection() {
        val latLng = selectedLatLng
        if (latLng == null) {
            Toast.makeText(this, "Vui l\u00f2ng ch\u1ecdn v\u1ecb tr\u00ed tr\u00ean b\u1ea3n \u0111\u1ed3", Toast.LENGTH_SHORT).show()
            return
        }
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(EXTRA_RESULT_LAT, latLng.latitude)
            putExtra(EXTRA_RESULT_LNG, latLng.longitude)
            putExtra(EXTRA_RESULT_ADDRESS, selectedAddress)
        })
        finish()
    }
}
