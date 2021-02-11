package com.ch8n.viewbinder.utils

fun appendBuildFeatureTemplate(before: String, after: String): String {
    return """
${before.trim()}

android {
    buildFeatures.viewBinding = true
    ${after.trim()}

""".trimIndent()
}
