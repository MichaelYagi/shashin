package com.miyagi.shashin

import java.util.*

class TextUtils {
    companion object {
        fun capitalized(str: String): String {
            return str.replaceFirstChar {
                if (it.isLowerCase())
                    it.titlecase(Locale.getDefault())
                else it.toString()
            }
        }
    }
}