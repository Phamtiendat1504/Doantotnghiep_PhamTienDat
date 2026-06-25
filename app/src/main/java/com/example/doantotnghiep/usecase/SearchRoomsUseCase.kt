package com.example.doantotnghiep.usecase

import com.example.doantotnghiep.AI.RoomScorer
import com.example.doantotnghiep.AI.SearchParams
import com.example.doantotnghiep.Model.AIRoom
import com.example.doantotnghiep.Utils.LocationNormalizer
import com.google.firebase.firestore.DocumentSnapshot

class SearchRoomsUseCase {

    data class ScoredResult(val room: AIRoom, val score: Int, val createdAt: Long)

    data class Result(
        val scored: List<ScoredResult>,
        val maxScore: Int
    )

    operator fun invoke(
        docs: List<DocumentSnapshot>,
        params: SearchParams
    ): Result {
        val now = System.currentTimeMillis()
        val maxScore = RoomScorer.computeMaxScore(params)

        val scored = docs.mapNotNull { doc ->
            val expiry = doc.getLong("postExpiryDate") ?: Long.MAX_VALUE
            if (expiry <= now) return@mapNotNull null

            if (params.district != null) {
                val normalizedSearch = LocationNormalizer.normalizeRaw(params.district)
                val docWardNorm = LocationNormalizer.normalizeRaw(doc.getString("ward") ?: "")
                val docDistrictNorm = LocationNormalizer.normalizeRaw(doc.getString("district") ?: "")
                if (docWardNorm != normalizedSearch && docDistrictNorm != normalizedSearch)
                    return@mapNotNull null
            }

            val score = RoomScorer.score(doc, params)
            if (score < 0) return@mapNotNull null

            val imageUrl = (doc.get("imageUrls") as? List<*>)
                ?.filterIsInstance<String>()?.firstOrNull() ?: ""
            ScoredResult(
                room = AIRoom(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    title = doc.getString("title") ?: "",
                    price = doc.getLong("price") ?: 0L,
                    area = (doc.getLong("area") ?: 0L).toInt(),
                    district = doc.getString("ward") ?: doc.getString("district") ?: "",
                    imageUrl = imageUrl
                ),
                score = score,
                createdAt = doc.getLong("createdAt") ?: 0L
            )
        }

        val sorted = if (params.newestFirst == true) {
            scored.sortedByDescending { it.createdAt }
        } else {
            scored.sortedByDescending { it.score }
        }

        return Result(scored = sorted, maxScore = maxScore)
    }
}
