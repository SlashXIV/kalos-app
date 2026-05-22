package com.kalos.app.core.data.util

import java.text.Normalizer

fun String.normalizeForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFKD)
        .replace(Regex("\\p{M}"), "")       // strip combining diacritics (é→e, ç→c…)
        .replace("œ", "oe").replace("Œ", "oe")
        .replace("æ", "ae").replace("Æ", "ae")
        .replace("ø", "o").replace("Ø", "o")
        .replace("ß", "ss")
        .lowercase()
        .trim()
