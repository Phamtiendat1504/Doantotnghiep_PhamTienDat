package com.example.doantotnghiep.AI

import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.util.Log
import com.example.doantotnghiep.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class GeocodingHelper(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val mapsApiKey: String by lazy { context.getString(R.string.google_maps_key) }

    suspend fun geocode(address: String): Pair<Double, Double>? =
        withContext(Dispatchers.IO) {
            val result = tryGoogleMapsApi(address)
                ?: trySystemGeocoder(address)
                ?: tryNominatim(address)
            result?.takeIf { (lat, lng) ->
                isInHanoi(lat, lng).also { inHanoi ->
                    if (!inHanoi) Log.w("GeocodingHelper", "Result ($lat,$lng) outside Hanoi bounds — discarded")
                }
            }
        }

    private fun isInHanoi(lat: Double, lng: Double): Boolean =
        lat in 20.5..21.4 && lng in 105.1..106.0

    private fun tryGoogleMapsApi(address: String): Pair<Double, Double>? {
        if (mapsApiKey.isBlank() || mapsApiKey == "YOUR_KEY_HERE") return null
        return try {
            val hasHanoi = address.contains("hà nội", ignoreCase = true) ||
                           address.contains("ha noi", ignoreCase = true)
            val query = if (hasHanoi) address else "$address, Hà Nội, Việt Nam"
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://maps.googleapis.com/maps/api/geocode/json" +
                "?address=$encoded&key=$mapsApiKey&language=vi"
            val response = httpClient.newCall(Request.Builder().url(url).build()).execute()
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            if (json.optString("status") != "OK") return null
            val results = json.getJSONArray("results")
            if (results.length() == 0) return null
            val location = results.getJSONObject(0)
                .getJSONObject("geometry").getJSONObject("location")
            val lat = location.getDouble("lat")
            val lng = location.getDouble("lng")
            Log.d("GeocodingHelper", "Google Maps → ($lat, $lng)")
            Pair(lat, lng)
        } catch (e: Exception) {
            Log.e("GeocodingHelper", "Google Maps API error", e)
            null
        }
    }

    private suspend fun trySystemGeocoder(address: String): Pair<Double, Double>? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale("vi", "VN"))
        val hasHanoi = address.contains("hà nội", ignoreCase = true) ||
                       address.contains("ha noi", ignoreCase = true)
        val query = if (hasHanoi) address else "$address, Hà Nội, Việt Nam"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocationName(query, 1) { addresses ->
                        val loc = addresses.firstOrNull()
                        cont.resume(if (loc != null) Pair(loc.latitude, loc.longitude) else null)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(query, 1)
                val loc = addresses?.firstOrNull() ?: run {
                    if (!hasHanoi) {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocationName("$address, Hà Nội, Việt Nam", 1)?.firstOrNull()
                    } else null
                }
                if (loc != null) {
                    Log.d("GeocodingHelper", "System Geocoder → (${loc.latitude}, ${loc.longitude})")
                    Pair(loc.latitude, loc.longitude)
                } else null
            }
        } catch (e: Exception) {
            Log.e("GeocodingHelper", "System Geocoder error", e)
            null
        }
    }

    private fun tryNominatim(address: String): Pair<Double, Double>? {
        val hasHanoi = address.contains("hà nội", ignoreCase = true) ||
                       address.contains("ha noi", ignoreCase = true)
        val q1 = if (hasHanoi) address else "$address, Hà Nội, Việt Nam"
        val noAccent = java.text.Normalizer.normalize(address, java.text.Normalizer.Form.NFD)
            .replace(Regex("[\\u0300-\\u036f\\u1dc0-\\u1dff]"), "")
            .replace('đ', 'd').replace('Đ', 'D')
        val q2 = if (hasHanoi) noAccent else "$noAccent, Hanoi, Vietnam"
        val coreName = address
            .replace(Regex("^(số\\s*\\d+[,\\s]*|ngõ\\s*\\d+[,\\s]*|ngách\\s*\\d+[,\\s]*)"), "").trim()
        val q3 = if (coreName != address) "$coreName, Hà Nội, Việt Nam" else null

        for (q in listOfNotNull(q1, q2, q3)) {
            try {
                val encoded = java.net.URLEncoder.encode(q, "UTF-8")
                val url = "https://nominatim.openstreetmap.org/search" +
                    "?q=$encoded&format=json&limit=1&accept-language=vi&countrycodes=vn"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "TimTro24_7_Android/1.0")
                    .build()
                val response = httpClient.newCall(request).execute()
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val arr = JSONArray(body)
                    if (arr.length() > 0) {
                        val lat = arr.getJSONObject(0).getDouble("lat")
                        val lng = arr.getJSONObject(0).getDouble("lon")
                        Log.d("GeocodingHelper", "OSM '$q' → ($lat, $lng)")
                        return Pair(lat, lng)
                    }
                }
                Thread.sleep(1100)
            } catch (e: Exception) {
                Log.e("GeocodingHelper", "OSM error for '$q'", e)
            }
        }
        Log.w("GeocodingHelper", "All geocoding strategies failed: $address")
        return null
    }
}
