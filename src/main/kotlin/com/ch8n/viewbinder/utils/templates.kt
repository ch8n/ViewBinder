package com.ch8n.viewbinder.utils

fun appendBuildFeatureGradle(before: String, after: String): String {
    return """
${before.trim()}
    buildFeatures {
        viewBinding = true
    ${after.trim()}

""".trimIndent()
}

fun appendBuildFeatureViewBindingTemplate(before: String, after: String): String {
    return """
${before.trim()}

android {
    buildFeatures.viewBinding = true
    ${after.trim()}

""".trimIndent()
}

fun appendImportViewBindingActivity(before: String, after: String, packageName: String): String {
    return """
$before
$packageName
import com.example.colorapp.base.ViewBindingActivity
$after    
    """.trimIndent()
}


