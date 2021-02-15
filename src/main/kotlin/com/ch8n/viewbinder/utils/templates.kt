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

fun appendImportViewBindingActivity(
    after: String,
    packageName: String,
    viewBindingClass: String
): String {
    return """
package $packageName

import $packageName.base.ViewBindingActivity
import $packageName.databinding.$viewBindingClass
$after    
    """.trimIndent()
}

fun removeAppCompatActivityImport(
    activityContent: String,
): String {
    val (before, after) = activityContent.split("import androidx.appcompat.app.AppCompatActivity")
    return """
    $before
    $after    
    """.trimIndent()
}




