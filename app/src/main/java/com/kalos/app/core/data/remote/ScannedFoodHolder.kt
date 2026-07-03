package com.kalos.app.core.data.remote

import com.kalos.app.core.domain.model.Food
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-shot in-memory hand-off of a scanned-and-resolved product from the food-search flow
 * to the custom-food editor, so the editor opens pre-filled with the OpenFoodFacts data.
 *
 * A Food carries several floats and an accented name; passing it through nav arguments would
 * mean URL-encoding a dozen fields. This singleton keeps the transfer clean and typed. The
 * consumer must read [pending] once and clear it immediately to avoid leaking into a later
 * manual creation.
 */
@Singleton
class ScannedFoodHolder @Inject constructor() {
    var pending: Food? = null
}
