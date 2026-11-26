package com.woorisseum.core.domain.household

/** 서버 계약과 동일한 White_Space 정리·Unicode 코드포인트 길이 정책이다. */
object ProfileName {
    fun normalize(value: String): String = value.trim { char ->
        char.code in 0x0009..0x000D || char.code in 0x2000..0x200A ||
            char.code in setOf(0x0020, 0x0085, 0x00A0, 0x1680, 0x2028, 0x2029, 0x202F, 0x205F, 0x3000)
    }

    fun isValid(value: String): Boolean {
        val normalized = normalize(value)
        var count = 0
        var index = 0
        while (index < normalized.length) {
            val code = normalized[index].code
            if (code in 0xD800..0xDBFF) {
                if (index + 1 >= normalized.length || normalized[index + 1].code !in 0xDC00..0xDFFF) return false
                index += 2
            } else {
                if (code in 0xDC00..0xDFFF) return false
                index++
            }
            count++
            if (count > 30) return false
        }
        return count in 1..30
    }
}
