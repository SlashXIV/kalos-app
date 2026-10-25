package com.kalos.app.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import com.kalos.app.R

/** Quantité lue par TalkBack avec son unité accordée : « 1 gramme », « 82 grammes » (singulier sous 2). */
@Composable
fun gramsSpoken(grams: Int): String = pluralStringResource(R.plurals.grams_spoken, grams, formatGroupedInt(grams))

/** « 0 kilocalorie », « 1 958 kilocalories ». */
@Composable
fun kcalSpoken(kcal: Int): String = pluralStringResource(R.plurals.me_kcal_spoken, kcal, formatGroupedInt(kcal))
