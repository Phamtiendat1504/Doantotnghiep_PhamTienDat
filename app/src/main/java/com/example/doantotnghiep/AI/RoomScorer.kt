package com.example.doantotnghiep.AI

import com.google.firebase.firestore.DocumentSnapshot

object RoomScorer {

    fun computeMaxScore(params: SearchParams): Int {
        var max = 0
        if (params.ward != null)      max += 3
        if (params.maxPrice != null)  max += 2
        if (params.minPrice != null)  max += 1
        if (params.minArea != null || params.maxArea != null) max += 1
        if (params.roomType != null)      max += 1
        if (params.genderPrefer != null)  max += 1
        if (params.maxPeopleCount != null) max += 1
        max += params.amenities.size
        return max
    }

    fun score(doc: DocumentSnapshot, params: SearchParams): Int {
        val docPrice = doc.getLong("price") ?: 0L

        // Fix #5: independent single-sided checks (previously only excluded when BOTH min AND max given)
        if (params.maxPrice != null && docPrice > params.maxPrice) return -1
        if (params.minPrice != null && docPrice < params.minPrice) return -1

        val docAmenities = buildAmenityList(doc)
        for (amenity in params.amenities) {
            if (!docAmenities.contains(amenity)) return -1
        }

        val docArea = (doc.getLong("area") ?: 0L).toInt()
        if (params.minArea != null && docArea < params.minArea) return -1
        if (params.maxArea != null && docArea > params.maxArea) return -1

        if (params.roomType != null) {
            val docType = doc.getString("roomType") ?: ""
            if (!docType.contains(params.roomType, ignoreCase = true)) return -1
        }

        if (params.genderPrefer != null) {
            // Tương thích cả 2 tên field: genderPrefer (Room.kt) và genderPreference (dữ liệu cũ)
            val docGender = doc.getString("genderPrefer")
                ?.takeIf { it.isNotEmpty() }
                ?: doc.getString("genderPreference")
                ?.takeIf { it.isNotEmpty() }
                ?: doc.getString("gender")
                ?: ""
            if (docGender.isNotEmpty() &&
                !docGender.contains("Tất cả", ignoreCase = true) &&
                !docGender.contains(params.genderPrefer, ignoreCase = true)) return -1
        }

        if (params.maxElectricPrice != null) {
            val docElec = doc.getLong("electricPrice") ?: 0L
            if (docElec > 0 && docElec > params.maxElectricPrice) return -1
        }

        if (params.maxWaterPrice != null) {
            val docWater = doc.getLong("waterPrice") ?: 0L
            if (docWater > 0 && docWater > params.maxWaterPrice) return -1
        }

        if (params.maxWifiPrice != null) {
            val docWifi = doc.getLong("wifiPrice") ?: 0L
            if (docWifi > 0 && docWifi > params.maxWifiPrice) return -1
        }

        if (params.maxDepositMonths != null) {
            val docDep = (doc.getLong("depositMonths") ?: 0L).toInt()
            if (docDep > params.maxDepositMonths) return -1
        }

        if (params.maxDepositAmount != null) {
            val docDepAmt = doc.getLong("depositAmount") ?: 0L
            if (docDepAmt > 0 && docDepAmt > params.maxDepositAmount) return -1
        }

        if (params.petAllowed != null) {
            val docPet = doc.getString("pet") ?: ""
            val docPetAllowed = docPet.contains("được", ignoreCase = true)
                || docPet.contains("thỏa thuận", ignoreCase = true)
                || docPet.contains("cho nuôi", ignoreCase = true)
                || docPet.contains("nuôi", ignoreCase = true)
            val docPetNotAllowed = docPet.contains("không", ignoreCase = true)
                || docPet.contains("cấm", ignoreCase = true)
                || docPet.contains("không cho", ignoreCase = true)
            when {
                // User muốn nuôi thú cưng: chỉ chấp nhận phòng có ghi rõ "được phép"
                params.petAllowed == true && !docPetAllowed && docPetNotAllowed -> return -1
                // User không muốn nuôi thú cưng: loại phòng ghi rõ "cho nuôi"
                params.petAllowed == false && docPetAllowed -> return -1
            }
        }

        if (params.freeTime != null) {
            val docCurfew = doc.getString("curfew") ?: ""
            // "Tùy chọn", "Tự do", "Không giới nghiêm", "Không chung chủ" đều được coi là free time
            val docFree = docCurfew.contains("tự do", ignoreCase = true) ||
                docCurfew.contains("không chung chủ", ignoreCase = true) ||
                docCurfew.contains("tùy chọn", ignoreCase = true) ||
                docCurfew.contains("tùy ý", ignoreCase = true) ||
                docCurfew.isEmpty()
            if (params.freeTime == true && !docFree) return -1
            // Nếu freeTime=false, vẫn để qua (không loại phòng có giờ giấc cụ thể)
        }

        if (params.maxPeopleCount != null) {
            val docPeople = (doc.getLong("peopleCount") ?: 0L).toInt()
            if (docPeople > 0 && docPeople < params.maxPeopleCount) return -1
        }

        if (params.daysAgo != null) {
            val docCreated = doc.getLong("createdAt") ?: 0L
            val diffMs = System.currentTimeMillis() - docCreated
            val diffDays = diffMs / (24L * 60 * 60 * 1000)
            if (diffDays > params.daysAgo) return -1
        }

        if (params.specificDate != null) {
            val docCreated = doc.getLong("createdAt") ?: 0L
            if (!isSameDay(docCreated, params.specificDate)) return -1
        }

        val docAddress = (doc.getString("address") ?: "").lowercase()
        var s = 0
        if (params.ward != null && docAddress.isNotEmpty() &&
            docAddress.contains(params.ward.lowercase())) s += 3
        if (params.maxPrice != null && docPrice <= params.maxPrice) s += 2
        if (params.minPrice != null && docPrice >= params.minPrice) s += 1
        for (amenity in params.amenities) {
            if (docAmenities.contains(amenity)) s += 1
        }
        if (params.minArea != null || params.maxArea != null) s += 1
        if (params.roomType != null) s += 1
        if (params.genderPrefer != null) s += 1
        return s
    }

    private fun buildAmenityList(doc: DocumentSnapshot): List<String> {
        val list = mutableListOf<String>()
        if (doc.getBoolean("hasWifi") == true)         list += "wifi"
        if (doc.getBoolean("hasAirCon") == true)       list += "airConditioner"
        if (doc.getBoolean("hasWasher") == true)       list += "washer"
        // Chỗ để xe — phân biệt rõ từng loại xe
        if (doc.getBoolean("hasParking") == true)      list += "parking"      // ô tô
        if (doc.getBoolean("hasMotorbike") == true)    list += "motorbike"    // xe máy
        if (doc.getBoolean("hasEBike") == true)        list += "ebike"        // xe đạp điện
        if (doc.getBoolean("hasBicycle") == true)      list += "bicycle"      // xe đạp
        if (doc.getBoolean("hasWaterHeater") == true)  list += "waterHeater"
        if (doc.getBoolean("hasWardrobe") == true)     list += "wardrobe"
        if (doc.getBoolean("hasBed") == true)          list += "bed"
        if (doc.getBoolean("hasDryingArea") == true)   list += "balcony"
        val kitchenVal = (doc.getString("kitchen") ?: "").lowercase()
        if (kitchenVal.contains("riêng") || kitchenVal.contains("co")) list += "kitchen"
        val bathroomVal = (doc.getString("bathroom") ?: "").lowercase()
        if (bathroomVal.contains("riêng") || bathroomVal.contains("co")) list += "privateWC"
        val furnitureNames = (doc.get("furnitureItems") as? List<*>)
            ?.mapNotNull { (it as? Map<*, *>)?.get("name") as? String }
            ?.map { it.lowercase() } ?: emptyList()
        val serviceNames = (doc.get("serviceItems") as? List<*>)
            ?.mapNotNull { (it as? Map<*, *>)?.get("name") as? String }
            ?.map { it.lowercase() } ?: emptyList()
        val allExtras = furnitureNames + serviceNames
        if (allExtras.any { it.contains("tủ lạnh") || it.contains("refrigerator") }) list += "refrigerator"
        if (allExtras.any { it.contains("tivi") || it.contains("tv") })               list += "tv"
        if (allExtras.any { it.contains("bảo vệ") || it.contains("camera") })         list += "security"
        // "furniture" = có đủ nội thất cơ bản (giường + tủ)
        if (doc.getBoolean("hasBed") == true && doc.getBoolean("hasWardrobe") == true) list += "furniture"
        return list
    }

    private fun isSameDay(createdAtMs: Long, dateStr: String): Boolean {
        return try {
            val sdf = java.text.SimpleDateFormat("d/M/yyyy", java.util.Locale("vi"))
            sdf.timeZone = java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
            
            val targetDate = sdf.parse(dateStr) ?: return false
            val calTarget = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"))
            calTarget.time = targetDate
            
            val calCreated = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"))
            calCreated.timeInMillis = createdAtMs
            
            calTarget.get(java.util.Calendar.YEAR) == calCreated.get(java.util.Calendar.YEAR) &&
            calTarget.get(java.util.Calendar.DAY_OF_YEAR) == calCreated.get(java.util.Calendar.DAY_OF_YEAR)
        } catch (e: Exception) {
            false
        }
    }
}
