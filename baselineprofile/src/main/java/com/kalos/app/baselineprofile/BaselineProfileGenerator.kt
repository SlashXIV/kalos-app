package com.kalos.app.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Génère le baseline profile de Kalos : les classes et méthodes parcourues ici sont
 * précompilées à l'installation au lieu d'être interprétées au premier lancement.
 *
 * Le parcours est celui de chaque ouverture : démarrage sur Aujourd'hui qui défile, puis
 * les onglets Sport et Calendrier. Rien d'autre : un profil trop large précompile du code froid.
 *
 * Lancement : ANDROID_SERIAL=emulator-5554 ./gradlew :app:generateBaselineProfile
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = TARGET_PACKAGE, includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()

        device.wait(Until.hasObject(By.scrollable(true)), WAIT_MS)
        device.findObject(By.scrollable(true))?.let { list ->
            list.scroll(Direction.DOWN, 1f)
            list.scroll(Direction.UP, 1f)
        }
        for (tab in listOf("SPORT", "CALENDRIER", "AUJOURD’HUI")) {
            device.findObject(By.text(tab))?.click()
            device.waitForIdle()
        }
    }
}

internal const val TARGET_PACKAGE = "com.kalos.app"
private const val WAIT_MS = 5_000L
