package com.ch8n.viewbinder.utils

fun appendBuildFeatureViewBindingTemplate(before: String, after: String): String {
    return """
${before.trim()}

android {
    buildFeatures.viewBinding = true
    ${after.trim()}

""".trimIndent()
}

fun appendBuildFeatureGradle(before: String, after: String): String {
    return """
${before.trim()}
    buildFeatures {
        viewBinding = true
    ${after.trim()}

""".trimIndent()
}


