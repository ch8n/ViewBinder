package com.ch8n.viewbinder

import com.ch8n.viewbinder.utils.appendBuildFeatureViewBindingTemplate
import com.ch8n.viewbinder.utils.appendViewBindingTemplate
import com.yg.kotlin.inquirer.components.promptConfirm
import com.yg.kotlin.inquirer.components.promptInput
import com.yg.kotlin.inquirer.components.promptListMultiObject
import com.yg.kotlin.inquirer.core.Choice
import com.yg.kotlin.inquirer.core.KInquirer
import java.io.File


object Config {
    val projectRoot: String = "/Users/chetangupta/StudioProjects/ColorChetan"
    val gradleEXT: String = ".gradle"
}


fun main() {

    val rootPath: String = KInquirer.promptInput("Please paste root project path : ")
    checkProjectExist(rootPath)

    val buildFiles = getBuildFiles(rootPath)
    if (buildFiles.isEmpty()) {
        println("no gradle build files found")
        return
    }

    val dependencyBuildFiles = getDependencyFiles(buildFiles)
    if (dependencyBuildFiles.isEmpty()) {
        println("dependencies gradle files found")
        return
    }

    val selectedModules = getModulesToInstallViewBinding(dependencyBuildFiles)
    if (selectedModules.isEmpty()) {
        println("No modules Selected")
        return
    }

    val isDependencyAdded = isViewBindingAddedAndSynced(selectedModules)
    if (!isDependencyAdded) {
        println("please sync your gradle and run script again!")
        return
    }

    //copy files
    addTemplateOfViewBindingActivity(rootPath)

}

fun addTemplateOfViewBindingActivity(rootPath: String) {
    val projectRoot = File(rootPath)
    projectRoot.walk()
}


fun isViewBindingAddedAndSynced(selectedModules: List<String>): Boolean {
    var output = """
    ----------------------    
    adding dependency to selected modules
    """.trimIndent()

    println(output)

    selectedModules.forEach {
        addViewBindingDependency(it)
    }

    output = """  
    adding view binding feature to selected modules!
    ----------------------
    """.trimIndent()

    println(output)

    val isGradleSynced: Boolean =
        KInquirer.promptConfirm("please sync you gradle, let me  know when it's done?", default = false)

    return isGradleSynced
}

fun getModulesToInstallViewBinding(dependencyBuildFiles: List<String>): List<String> {

    val moduleName = dependencyBuildFiles.map {
        Choice<String>(it.split("/").takeLast(2).get(0), it)
    }

    if (moduleName.isEmpty()) {
        println("No modules found")
        return emptyList()
    }

    val selectedModules: List<String> =
        KInquirer.promptListMultiObject("[space to select] : select module to install?", moduleName)

    var output = """
    ----------------------    
    Selected modules:
    ${selectedModules.joinToString(separator = "\n").trimIndent()}
    ----------------------
    """.trimIndent()
    println(output)

    return selectedModules
}

fun getDependencyFiles(buildFiles: List<String>): List<String> {
    val dependencyBuildFiles = buildFiles.filter {
        val moduleBuilds = it.split(Config.projectRoot).getOrNull(1) ?: ""
        val isModuleBuildFiles = moduleBuilds.split("/").size == 3
        isModuleBuildFiles
    }

    var output = """
    ----------------------    
    Dependencies files found :
    ${dependencyBuildFiles.joinToString(separator = "\n").trimIndent()}
    ----------------------
    """.trimIndent()

    println(output)

    return dependencyBuildFiles
}

fun getBuildFiles(rootPath: String): List<String> {

    val buildFiles = File(rootPath).walk()
        .filter { it.path.endsWith(Config.gradleEXT) }
        .filter { !it.path.split("/").last().split(Config.gradleEXT).firstOrNull().isNullOrBlank() }
        .map { it.path }
        .toList()

    var output = """
    ----------------------    
    Gradle Build files found : 
    ${buildFiles.joinToString(separator = "\n").trimIndent()}
    ----------------------
    """.trimIndent()

    println(output)
    return buildFiles
}

fun checkProjectExist(rootPath: String) {
    val projectRoot = File(rootPath)
    val existing = projectRoot.exists()
    var output = """
    ----------------------    
    Root project found? 
    ${existing}
    ----------------------
    """.trimIndent()
    println(output)

    if (!existing) {
        return
    }
}


fun addViewBindingDependency(modulePath: String) {
    val oneFile = File(modulePath)
    val content = oneFile.readText(Charsets.UTF_8)

    when {
        content.contains("buildFeatures") -> {
            if (!content.contains("viewBinding")) {
                val (before, after) = content.split("buildFeatures {")
                val updatedContent = appendViewBindingTemplate(before, after)
                println(updatedContent)
                oneFile.bufferedWriter().use { out ->
                    out.write(updatedContent)
                }
            }
        }
        else -> {
            val (before, after) = content.split("android {")
            val updatedContent = appendBuildFeatureViewBindingTemplate(before, after)
            println(updatedContent)
            oneFile.bufferedWriter().use { out ->
                out.write(updatedContent)
            }
        }
    }

}
