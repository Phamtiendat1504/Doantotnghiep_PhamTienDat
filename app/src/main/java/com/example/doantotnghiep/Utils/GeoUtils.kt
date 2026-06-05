package com.example.doantotnghiep.Utils

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeoUtils {

    private const val EARTH_RADIUS_KM = 6371.0
    // 1 độ vĩ tuyến ≈ 111.32 km
    private const val KM_PER_LAT_DEGREE = 111.32

    fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).let { it * it } +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2).let { it * it }
        return EARTH_RADIUS_KM * 2 * asin(sqrt(a))
    }

    /** Độ lệch vĩ độ tương ứng với radiusKm — dùng để tạo bounding box trên Firestore. */
    fun latDelta(radiusKm: Double): Double = radiusKm / KM_PER_LAT_DEGREE
}
