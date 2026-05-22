package com.kalos.app.core.data.util

import java.text.Normalizer

fun String.normalizeForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFKD)
        .replace(Regex("\\p{M}"), "")       // strip combining diacritics (é→e, ç→c…)
        .replace('œ', 'o').replace('Œ', 'O')
        .replace('æ', 'a').replace('Æ', 'A')
        .replace('ø', 'o').replace('Ø', 'O')
        .replace('ß', 's')
        .lowercase()
        .trim()
