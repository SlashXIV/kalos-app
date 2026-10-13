package com.kalos.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Aucun texte d'interface codé en dur : les textes vivent dans `strings.xml`, où s'écrit un
 * français correct (accents, apostrophe typographique, pluriels). Détection volontairement simple,
 * par motifs (reprise de Dromos) : un littéral contenant au moins une lettre, passé à `Text(`,
 * `showSnackbar(` ou à un paramètre qui porte du texte visible. Les gabarits `$x` / `${x}` sont
 * retirés avant de chercher une lettre. Faux positif : `// texte-technique : <raison>` en fin de
 * ligne, raison obligatoire. Le `label` d'une animation Compose n'est jamais affiché : ignoré.
 */
class HardcodedUiTextTest {

    @Test
    fun `aucun texte visible code en dur hors des ecrans en attente de migration`() {
        val root = File("src/main/java")
        val findings = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.relativeTo(root).invariantSeparatorsPath in PENDING }
            .sortedBy { it.path }
            .flatMap { f -> findHardcodedUiText(f.readLines()).map { (n, code) -> "${f.relativeTo(root)}:$n  $code" } }
            .toList()
        assertTrue(
            "Texte visible codé en dur (à déplacer dans strings.xml, ou « // texte-technique : raison ») :\n" +
                findings.joinToString("\n"),
            findings.isEmpty(),
        )
    }

    @Test
    fun `les ecrans en attente existent encore`() {
        // Un fichier migré ou renommé doit sortir de la liste : elle ne fait que rétrécir.
        val missing = PENDING.filterNot { File("src/main/java/$it").exists() }
        assertEquals(emptyList<String>(), missing)
    }

    @Test
    fun `le detecteur repere les textes visibles et ignore le reste`() {
        val lines = listOf(
            """Text("Bonjour")""",
            """Icon(x, contentDescription = "Retour")""",
            """Chip(label = "Protéines")""",
            """Text("${'$'}{done}/${'$'}{total}")""",
            """Text(stringResource(R.string.ok))""",
            """// Text("commentaire")""",
            """Text("kcal") // texte-technique : unité""",
            """val a by animateFloatAsState(1f,""",
            """    label = "aiguille")""",
        )
        assertEquals(listOf(1, 2, 3), findHardcodedUiText(lines).map { it.first })
    }

    private companion object {
        /** Fichiers pas encore passés dans le nouveau langage : la liste doit être vide en 4.0.0. */
        val PENDING: Set<String> = File("src/test/resources/hardcoded-ui-pending.txt")
            .takeIf { it.exists() }?.readLines()?.map { it.trim() }?.filter { it.isNotEmpty() && !it.startsWith("#") }
            ?.toSet().orEmpty()

        val VISIBLE_TEXT = Regex(
            """(?:\bText\(\s*|\bshowSnackbar\(\s*|\b(contentDescription|title|subtitle|message|label|hint|""" +
                """actionLabel|confirmLabel|text|placeholder|description)\s*=\s*)"((?:[^"\\]|\\.)*)"""",
        )
        val TEMPLATE = Regex("""\$\{[^}]*}|\$[A-Za-z_][A-Za-z0-9_]*""")
        val OPT_OUT = Regex("""//\s*texte-technique\s*:\s*\S""")
        val ANIMATION_CALL = Regex("""\b(?:animate\w*|\w*Transition|AnimatedVisibility|AnimatedContent|Crossfade)\s*\(""")
        const val ANIMATION_LOOKBACK = 3

        fun findHardcodedUiText(lines: List<String>): List<Pair<Int, String>> =
            lines.withIndex().mapNotNull { (index, line) ->
                val code = line.trimStart()
                if (code.startsWith("//") || code.startsWith("*") || code.startsWith("/*")) return@mapNotNull null
                if (OPT_OUT.containsMatchIn(line)) return@mapNotNull null
                val hardcoded = VISIBLE_TEXT.findAll(line).any { match ->
                    val (name, text) = match.destructured
                    TEMPLATE.replace(text, "").any(Char::isLetter) && !(name == "label" && inAnimationCall(lines, index))
                }
                if (hardcoded) index + 1 to code else null
            }

        fun inAnimationCall(lines: List<String>, index: Int): Boolean =
            (maxOf(0, index - ANIMATION_LOOKBACK)..index).any { ANIMATION_CALL.containsMatchIn(lines[it]) }
    }
}
