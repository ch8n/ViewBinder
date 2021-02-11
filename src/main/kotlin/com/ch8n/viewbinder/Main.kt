package com.ch8n.viewbinder

import com.ch8n.viewbinder.utils.appendBuildFeatureTemplate
import java.io.File

object Config {
    val projectRoot: String = "/Users/chetangupta/StudioProjects/ColorChetan"
    val gradleEXT: String = ".gradle"
}


fun main() {
    val projectRoot = File(Config.projectRoot)
    println(projectRoot.exists())

    val buildFiles = projectRoot.walk()
        .filter { it.path.endsWith(Config.gradleEXT) }
        .filter { !it.path.split("/").last().split(Config.gradleEXT).firstOrNull().isNullOrBlank() }
        .map { it.path }
        .toList()

    println("-------All build files-------")
    println(buildFiles.joinToString(separator = "\n"))
    val dependencyBuildFiles = buildFiles.filter {
        val moduleBuilds = it.split(Config.projectRoot).getOrNull(1) ?: ""
        val isModuleBuildFiles = moduleBuilds.split("/").size == 3
        isModuleBuildFiles
    }
    println("-------module build files-------")
    println(dependencyBuildFiles.joinToString(separator = "\n"))

    val oneFile = File(requireNotNull(dependencyBuildFiles.getOrNull(0)))
    val content = oneFile.readText(Charsets.UTF_8)

    when {
        content.contains("buildFeatures") -> {
            if (!content.contains("viewBinding")) {

            }
        }
        else -> {
            // append buildFeatures.viewBinding = true
            val (before, after) = content.split("android {")
            val updatedContent = appendBuildFeatureTemplate(before, after)
            println(updatedContent)
            oneFile.bufferedWriter().use { out ->
                out.write(updatedContent)
            }
        }
    }


}

