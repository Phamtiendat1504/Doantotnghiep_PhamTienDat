package com.example.doantotnghiep.Utils

import java.text.Normalizer
import java.util.Locale

object LocationNormalizer {

    private val wardAliasMap: Map<String, String> = mapOf(
        // Nhom ward cu -> ward moi (Ha Noi sap xep 2025)
        "hang bac" to "hoan kiem",
        "hang bo" to "hoan kiem",
        "hang buom" to "hoan kiem",
        "hang dao" to "hoan kiem",
        "hang gai" to "hoan kiem",
        "hang ma" to "hoan kiem",
        "ly thai to" to "hoan kiem",
        "dong xuan" to "hoan kiem",
        "cua dong" to "hoan kiem",
        "trang tien" to "hoan kiem",
        "hang bong" to "hoan kiem",
        "hang trong" to "hoan kiem",
        "hang bai" to "cua nam",
        "phan chu trinh" to "cua nam",
        "tran hung dao" to "cua nam",
        "pham dinh ho" to "cua nam",
        "quan thanh" to "ba dinh",
        "truc bach" to "ba dinh",
        "vinh phuc" to "ngoc ha",
        "lieu giai" to "ngoc ha",
        "giang vo" to "giang vo",
        "dong nhan" to "hai ba trung",
        "pho hue" to "hai ba trung",
        "bach mai" to "bach mai",
        "bach khoa" to "bach mai",
        "quynh mai" to "bach mai",
        "thinh quang" to "dong da",
        "kim lien" to "kim lien",
        "khuong thuong" to "kim lien",
        "kham thien" to "van mieu quoc tu giam",
        "tho quan" to "van mieu quoc tu giam",
        "van chuong" to "van mieu quoc tu giam",
        "lang thuong" to "lang",
        "cat linh" to "o cho dua",
        "chuong duong" to "hong ha",
        "phuc tan" to "hong ha",
        "phuc xa" to "hong ha",
        "linh nam" to "linh nam",
        "hoang liet" to "hoang mai",
        "giap bat" to "hoang mai",
        "vinh hung" to "vinh hung",
        "tuong mai" to "tuong mai",
        "dinh cong" to "dinh cong",

        // Ward moi -> chinh no (de so sanh 2 chieu on dinh)
        "hoan kiem" to "hoan kiem",
        "cua nam" to "cua nam",
        "ba dinh" to "ba dinh",
        "ngoc ha" to "ngoc ha",
        "giang vo" to "giang vo",
        "hai ba trung" to "hai ba trung",
        "vinh tuy" to "vinh tuy",
        "bach mai" to "bach mai",
        "dong da" to "dong da",
        "kim lien" to "kim lien",
        "van mieu quoc tu giam" to "van mieu quoc tu giam",
        "lang" to "lang",
        "o cho dua" to "o cho dua",
        "hong ha" to "hong ha",
        "linh nam" to "linh nam",
        "hoang mai" to "hoang mai",
        "vinh hung" to "vinh hung",
        "tuong mai" to "tuong mai",
        "dinh cong" to "dinh cong"
    )

    fun normalizeRaw(value: String): String {
        return removeAccents(value.trim())
            .replace(Regex("\\s+"), " ")
            .lowercase(Locale.ROOT)
    }

    fun normalizeWard(value: String): String {
        val key = normalizeRaw(value)
        return wardAliasMap[key] ?: key
    }

    fun normalizeDistrict(value: String): String {
        return normalizeRaw(value)
    }

    private fun removeAccents(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        return normalized
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace("đ", "d")
            .replace("Đ", "D")
    }
}

