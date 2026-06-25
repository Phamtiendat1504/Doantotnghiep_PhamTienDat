package com.example.doantotnghiep.usecase

import com.example.doantotnghiep.Utils.GeoUtils
import com.example.doantotnghiep.AI.RoomScorer
import com.example.doantotnghiep.AI.SearchParams
import com.example.doantotnghiep.Model.AIRoom
import com.google.firebase.firestore.DocumentSnapshot

class GeoSearchRoomsUseCase {

    data class ScoredRoom(val room: AIRoom, val score: Int, val distKm: Double)

    data class Result(
        val rooms: List<AIRoom>,
        val relaxedRooms: List<AIRoom>,
        val usedRelaxed: Boolean
    )

    operator fun invoke(
        docs: List<DocumentSnapshot>,
        lat: Double,
        lng: Double,
        radiusKm: Double,
        params: SearchParams
    ): Result {
        val allScored = scoreWithGeo(docs, lat, lng, radiusKm, params)
        val sorted = sortScored(allScored, params)

        if (sorted.isNotEmpty()) {
            return Result(
                rooms = sorted.map { it.room }.take(8),
                relaxedRooms = emptyList(),
                usedRelaxed = false
            )
        }

        if (params.amenities.isEmpty()) {
            return Result(rooms = emptyList(), relaxedRooms = emptyList(), usedRelaxed = false)
        }

        val relaxedScored = scoreWithGeo(docs, lat, lng, radiusKm, params.copy(amenities = emptyList()))
        val relaxedSorted = sortScored(relaxedScored, params).take(5)
        return Result(
            rooms = emptyList(),
            relaxedRooms = relaxedSorted.map { it.room },
            usedRelaxed = true
        )
    }

    private fun scoreWithGeo(
        docs: List<DocumentSnapshot>,
        lat: Double,
        lng: Double,
        radiusKm: Double,
        params: SearchParams
    ): List<ScoredRoom> = docs.mapNotNull { doc ->
        val docLat = doc.getDouble("latitude") ?: return@mapNotNull null
        val docLng = doc.getDouble("longitude") ?: return@mapNotNull null
        val distKm = GeoUtils.haversineKm(lat, lng, docLat, docLng)
        if (distKm > radiusKm) return@mapNotNull null

        val criteriaScore = RoomScorer.score(doc, params)
        if (criteriaScore < 0) return@mapNotNull null

        val proximity = when {
            distKm <= radiusKm * 0.25 -> 5
            distKm <= radiusKm * 0.5  -> 4
            distKm <= radiusKm * 0.75 -> 3
            else                      -> 2
        }

        val imageUrl = (doc.get("imageUrls") as? List<*>)
            ?.filterIsInstance<String>()?.firstOrNull() ?: ""
        ScoredRoom(
            room = AIRoom(
                id = doc.id,
                userId = doc.getString("userId") ?: "",
                title = doc.getString("title") ?: "",
                price = doc.getLong("price") ?: 0L,
                area = (doc.getLong("area") ?: 0L).toInt(),
                district = doc.getString("ward") ?: doc.getString("district") ?: "",
                imageUrl = imageUrl
            ),
            score = proximity + criteriaScore,
            distKm = distKm
        )
    }

    private fun sortScored(list: List<ScoredRoom>, params: SearchParams): List<ScoredRoom> =
        if (params.newestFirst == true) list
        else list.sortedWith(compareByDescending<ScoredRoom> { it.score }.thenBy { it.distKm })
}
