package com.ch8n.viewbinder

import com.ch8n.viewbinder.utils.Command
import com.ch8n.viewbinder.utils.appendBuildFeatureViewBindingTemplate
import com.ch8n.viewbinder.utils.appendViewBindingTemplate
import com.yg.kotlin.inquirer.components.promptConfirm
import com.yg.kotlin.inquirer.components.promptInput
import com.yg.kotlin.inquirer.components.promptList
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


    val buildFiles = projectRoot.walk()
        .filter { it.path.endsWith(Config.gradleEXT) }
        .filter { !it.path.split("/").last().split(Config.gradleEXT).firstOrNull().isNullOrBlank() }
        .map { it.path }
        .toList()

    output = """
    ----------------------    
    Gradle Build files found : 
    ${buildFiles.joinToString(separator = "\n").trimIndent()}
    ----------------------
    """.trimIndent()

    println(output)

    if (buildFiles.isEmpty()) {
        println("no gradle build files found")
        return
    }

    val dependencyBuildFiles = buildFiles.filter {
        val moduleBuilds = it.split(Config.projectRoot).getOrNull(1) ?: ""
        val isModuleBuildFiles = moduleBuilds.split("/").size == 3
        isModuleBuildFiles
    }

    output = """
    ----------------------    
    Dependencies files found :
    ${dependencyBuildFiles.joinToString(separator = "\n").trimIndent()}
    ----------------------
    """.trimIndent()

    println(output)

    if (dependencyBuildFiles.isEmpty()) {
        println("dependencies gradle files found")
        return
    }

    val moduleName = dependencyBuildFiles.map {
        Choice<String>(it.split("/").takeLast(2).get(0), it)
    }

    if (moduleName.isEmpty()) {
        println("No modules found")
        return
    }

    val selectedModules: List<String> =
        KInquirer.promptListMultiObject("[space to select] : select module to install?", moduleName)

    output = """
    ----------------------    
    Selected modules:
    ${selectedModules.joinToString(separator = "\n").trimIndent()}
    ----------------------
    """.trimIndent()
    println(output)

    if (selectedModules.isEmpty()) {
        println("No modules Selected")
        return
    }

    output = """
    ----------------------    
    adding dependency to selected modules:
    ----------------------
    """.trimIndent()

    println(output)

    selectedModules.forEach {
        addViewBindingDependency(it)
    }

    output = """
    ----------------------    
    adding view binding feature to selected modules!
    ----------------------
    """.trimIndent()

    println(output)

    val isGradleSynced: Boolean = KInquirer.promptConfirm("please sync you gradle, let me  know when it's done?", default = false)

    if (!isGradleSynced){
        println("please sync your gradle and run script again!")
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
