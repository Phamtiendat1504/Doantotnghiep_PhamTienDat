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

    // 'operator fun invoke' là một tính năng đặc biệt của Kotlin. 
    // Nó cho phép ta gọi Class GeoSearchRoomsUseCase giống hệt như gọi một hàm: GeoSearchRoomsUseCase(...)
    operator fun invoke(
        docs: List<DocumentSnapshot>, // Danh sách toàn bộ phòng trọ thô (chưa lọc) từ Firebase
        lat: Double,                  // Vĩ độ GPS của người dùng
        lng: Double,                  // Kinh độ GPS của người dùng
        radiusKm: Double,             // Bán kính tìm kiếm (km)
        params: SearchParams          // Các bộ lọc tiêu chí khắt khe (Giá, Tiện ích wifi/điều hòa...)
    ): Result {
        
        // --- LƯỢT TÌM KIẾM 1: TÌM KIẾM NGHIÊM NGẶT (STRICT SEARCH) ---
        // Chấm điểm tất cả các phòng trọ dựa trên khoảng cách và ĐẦY ĐỦ các tiêu chí người dùng nhập
        val allScored = scoreWithGeo(docs, lat, lng, radiusKm, params)
        
        // Sắp xếp danh sách dựa trên Tổng điểm (từ cao xuống thấp)
        val sorted = sortScored(allScored, params)

        // Nếu tìm thấy phòng trọ thỏa mãn TOÀN BỘ tiêu chí:
        if (sorted.isNotEmpty()) {
            return Result(
                rooms = sorted.map { it.room }.take(8), // Chỉ lấy tối đa 8 phòng có điểm cao nhất
                relaxedRooms = emptyList(),             // Không cần dùng đến danh sách gợi ý nới lỏng
                usedRelaxed = false                     // Báo cho UI biết là kết quả trả về chuẩn 100%
            )
        }

        // Nếu KHÔNG tìm thấy phòng nào, và người dùng cũng KHÔNG yêu cầu tiện ích gì thêm:
        // (Tức là hết cách cứu chữa, đành phải trả về danh sách rỗng)
        if (params.amenities.isEmpty()) {
            return Result(rooms = emptyList(), relaxedRooms = emptyList(), usedRelaxed = false)
        }

        // -ƯỢT TÌM KIẾM 2: TÌM KIẾM NỚI LỎNG (RELAXED SEARCH) - CỨU CÁNH UX
        // Chạy đến đây nghĩa là: Khu vực này không có phòng nào thỏa mãn TẤT CẢ tiện ích người dùng cần.
        // Thay vì báo "Không tìm thấy phòng", hệ thống tự động "Nới lỏng": Xóa bỏ yêu cầu về tiện ích (amenities = emptyList)
        val relaxedScored = scoreWithGeo(docs, lat, lng, radiusKm, params.copy(amenities = emptyList()))
        
        // Sắp xếp lại và lấy 5 phòng tốt nhất ở gần đó (Dù thiếu tiện ích nhưng bù lại vị trí tốt)
        val relaxedSorted = sortScored(relaxedScored, params).take(5)
        
        return Result(
            rooms = emptyList(),                           // Kết quả chuẩn = 0
            relaxedRooms = relaxedSorted.map { it.room },  // Trả về danh sách 5 phòng "Gợi ý thay thế"
            usedRelaxed = true                             // Báo cho UI hiện dòng chữ "Không có phòng đúng ý, thử xem các phòng gần đó"
        )
    }

    // Hàm cốt lõi: Tính toán khoảng cách và Chấm điểm (Scoring) cho từng phòng trọ
    private fun scoreWithGeo(
        docs: List<DocumentSnapshot>, // Danh sách phòng trọ thô kéo từ Firebase
        lat: Double,                  // Vĩ độ GPS của người dùng
        lng: Double,                  // Kinh độ GPS của người dùng
        radiusKm: Double,             // Bán kính tìm kiếm tối đa (ví dụ: 2km)
        params: SearchParams          // Các bộ lọc khác (Giá, Diện tích, Tiện ích...)
    ): List<ScoredRoom> = docs.mapNotNull { doc ->
        
        // 1. Lấy tọa độ của phòng trọ hiện tại đang xét
        val docLat = doc.getDouble("latitude") ?: return@mapNotNull null
        val docLng = doc.getDouble("longitude") ?: return@mapNotNull null
        
        // 2. Dùng thuật toán Haversine tính khoảng cách thực tế từ người dùng đến phòng trọ
        val distKm = GeoUtils.haversineKm(lat, lng, docLat, docLng)
        
        // Nếu phòng trọ nằm ngoài bán kính tìm kiếm -> Bỏ qua luôn (không chấm điểm nữa)
        if (distKm > radiusKm) return@mapNotNull null

        // 3. Chấm điểm Tiêu chí (Criteria Score): Chấm điểm dựa trên Giá, Diện tích, Tiện ích... thông qua AI/RoomScorer
        val criteriaScore = RoomScorer.score(doc, params)
        
        // Nếu điểm tiêu chí < 0 (nghĩa là phòng này vi phạm điều kiện bắt buộc, ví dụ vượt quá mức giá tối đa) -> Bỏ qua luôn
        if (criteriaScore < 0) return@mapNotNull null

        // 4. Chấm điểm Vị trí (Proximity Score): Trọng số ưu tiên phòng ở GẦN HƠN
        val proximity = when {
            distKm <= radiusKm * 0.25 -> 5 // Nằm trong 25% bán kính đầu tiên (Rất gần) -> Thưởng 5 điểm
            distKm <= radiusKm * 0.5  -> 4 // Nằm trong nửa bán kính -> Thưởng 4 điểm
            distKm <= radiusKm * 0.75 -> 3 // Nằm trong 75% bán kính -> Thưởng 3 điểm
            else                      -> 2 // Khá xa (nhưng vẫn trong bán kính) -> Thưởng 2 điểm
        }

        // Lấy ảnh bìa của phòng trọ để hiển thị lên danh sách
        val imageUrl = (doc.get("imageUrls") as? List<*>)
            ?.filterIsInstance<String>()?.firstOrNull() ?: ""
            
        // 5. Trả về Object ScoredRoom chứa tất cả thông tin để chuẩn bị xếp hạng
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
            // TỔNG ĐIỂM CUỐI CÙNG = Điểm Vị trí + Điểm Tiêu chí (AI)
            // Tổng điểm càng cao, phòng trọ càng được xếp lên đầu tiên ở màn hình kết quả
            score = proximity + criteriaScore, 
            distKm = distKm
        )
    }

    private fun sortScored(list: List<ScoredRoom>, params: SearchParams): List<ScoredRoom> =
        if (params.newestFirst == true) list
        else list.sortedWith(compareByDescending<ScoredRoom> { it.score }.thenBy { it.distKm })
}
