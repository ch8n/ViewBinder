package com.ch8n.viewbinder


import com.ch8n.viewbinder.utils.appendBuildFeatureViewBindingTemplate
import com.ch8n.viewbinder.utils.appendBuildFeatureGradle
import com.yg.kotlin.inquirer.components.promptConfirm
import com.yg.kotlin.inquirer.components.promptInput
import com.yg.kotlin.inquirer.core.Choice
import com.yg.kotlin.inquirer.core.KInquirer
import java.io.File
import java.io.FileWriter


object Config {
    val projectRoot: String = "/Users/chetangupta/StudioProjects/ColorChetan"
    val gradleEXT: String = ".gradle"
}


fun main() {

    val activity =
        listOf(File("/Users/chetangupta/StudioProjects/ColorChetan/app/src/main/java/com/example/colorapp/MainActivity.kt"))
    updateSuperClassToViewBind(activity, Config.projectRoot)

    return


    val rootPath: String = KInquirer.promptInput(
        message = "Please paste root project path : ",
        default = "/Users/chetangupta/StudioProjects/ColorChetan"
    )
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

    addTemplateOfViewBindingActivity(selectedModules.get(0))
    val activities = scanActivities(selectedModules.get(0))
    if (activities.isEmpty()) {
        println("no activity to modify!!")
        return
    }

    updateSuperClassToViewBind(activities, Config.projectRoot)

}

fun updateSuperClassToViewBind(activities: List<File>, projectRoot: String) {
    // todo solve for multiple class
    val activity = activities.get(0)
    val activityContent = activity.readText(Charsets.UTF_8)

    if (activityContent.contains("ViewBindingActivity<")) {
        println("ViewBindingActivity already found in ${activity.path}")
        return
    }

    val activityLines = activityContent.reader().readLines().toMutableList()
    val packageNameLine = activityLines.first { it.contains("package") }
    val (_, packageName) = packageNameLine.split("package")
    println(packageName)

    val layoutLine = activityLines.first { it.contains("R.layout") }
    val layoutName = layoutLine.split(".").last().dropLast(1)
    println(layoutName)

    val viewBindingClassName =
        layoutName.split("_").joinToString(separator = "") { it.capitalize() }.let { "${it}Binding" }
    println(viewBindingClassName)

    //------ working on imports ----//
    // add base view binding imports
    val syntheticImportIndex = activityLines.indexOfFirst { it.contains("kotlinx.android.synthetic") }
    activityLines.removeAt(syntheticImportIndex)
    activityLines.add(
        syntheticImportIndex, """
    import$packageName.base.ViewBindingActivity
    import$packageName.databinding.$viewBindingClassName
    import android.view.LayoutInflater
    """.trimIndent()
    )
    // remove appCompact remote
    val appCompactImportIndex = activityLines.indexOfFirst { it.contains("androidx.appcompat.app.AppCompatActivity") }
    activityLines.removeAt(appCompactImportIndex)

    //replace parent activity with viewbinding Activity
    val superClassLineIndex = activityLines.indexOfFirst { it.contains(": AppCompatActivity") }
    val viewBindSuperClass = activityLines.get(superClassLineIndex)
        .replace("AppCompatActivity", "ViewBindingActivity<$viewBindingClassName>")
    activityLines.removeAt(superClassLineIndex)
    activityLines.add(superClassLineIndex, viewBindSuperClass)

    // add viewbinding inflater method
    activityLines.add(
        superClassLineIndex + 1, """
            
            override val bindingInflater: (LayoutInflater) -> $viewBindingClassName
                get() = $viewBindingClassName::inflate
            """.trimIndent()
    )

    // replace onCreate with setup()
    val indexSuperOnCreate = activityLines.indexOfFirst { it.contains("super.onCreate") }
    activityLines.removeAt(indexSuperOnCreate)

    val indexOnCreateFunction = activityLines.indexOfFirst { it.contains("fun onCreate(savedInstanceState") }
    val setupFunction =
        activityLines.get(indexOnCreateFunction).replace("onCreate(savedInstanceState: Bundle?)", "setup()")
    activityLines.removeAt(indexOnCreateFunction)
    activityLines.add(indexOnCreateFunction, setupFunction)

    // remove content view
    val indexContentView = activityLines.indexOfFirst { it.contains("setContentView") }
    activityLines.removeAt(indexContentView)

    // find layout file
    val layoutPath = "$projectRoot/app/src/main/res/layout"
    val layoutFiles = File(layoutPath)
    if (!layoutFiles.exists()) {
        println("cant find layout folder")
    }

    val fileName = layoutFiles.walk()
        .first {
            it.path.contains(layoutName)
        }

    val layoutIds: List<Pair<String/*layoutID*/, String/*bindingID*/>> = fileName.readText()
        .lines()
        .filter { it.contains("android:id=\"@+id/") }
        .map {
            it.split("android:id=\"@+id/")[1].dropLast(1)
        }.map { id ->
            val words = id.split("_")
            val first = words.take(1)
            val others = words.drop(1).map { it.capitalize() }
            id to (first + others).joinToString(separator = "")
        }

    println(layoutIds)

    // hack to concurrent override values
    var bindingActivity = activityLines.toList()
    layoutIds.forEach { (layoutId, bindingId) ->
        println("$layoutId : $bindingId")
        bindingActivity.mapIndexed { index, line ->
            if (line.contains(layoutId)) {
                val updatedLine = line.replace(layoutId, "binding.$bindingId")
                activityLines.removeAt(index)
                activityLines.add(index, updatedLine)
            }
            bindingActivity = activityLines.toList()
        }
    }

    val activityWithBindings = bindingActivity.toMutableList()


    val indexSuperClass = activityWithBindings.indexOfFirst { it.contains("ViewBindingActivity<") }
    activityWithBindings.add(
        indexSuperClass - 1, """
        /***
        *   Migrated using Chetan-Gupta : AndroidBites View-binding Migrator
        *   Thanks you for using my tool! , please leave bug reports and feature request
        ****/
    """.trimIndent()
    )

    println(activityWithBindings.joinToString(separator = "\n"))
    val fileWriter = FileWriter(activity, false)
    fileWriter.write(activityWithBindings.joinToString(separator = "\n"))
    fileWriter.close()


}

fun scanActivities(moduleGradlePath: String): List<File> {
    val modulePath = moduleGradlePath.split("/").dropLast(1).joinToString(separator = "/")
    val module = File(modulePath)
    val files = module.walk()
        .filter { it.path.contains("app/src/main/java") }
        .toList()

    var output = """
    ----------------------    
    "finding modules files..."
    ${files.joinToString(separator = "\n").trimIndent()}
    ----------------------
    """.trimIndent()

    println(output)

    val kotlinFiles = files
        .filter { it.path.contains(".kt") }

    output = """
    ----------------------    
    "Found Kotlin files..."
    ${kotlinFiles.joinToString(separator = "\n")}
    ----------------------
    """.trimIndent()

    println(output)

    val activities = kotlinFiles.filter {
        val content = it.readText(Charsets.UTF_8)
        !it.path.contains("base") && content.contains("AppCompatActivity")
    }

    output = """
    ----------------------    
    "Found Activities files..."
    ${activities.joinToString(separator = "\n")}
    ----------------------
    """.trimIndent()

    println(output)

    return activities

}

fun addTemplateOfViewBindingActivity(moduleGradlePath: String) {
    val modulePath = moduleGradlePath.split("/").dropLast(1).joinToString(separator = "/")
    val module = File(modulePath)
    val files = module.walk()
        .filter { it.path.contains("app/src/main/java") }
        .toList()

    var output = """
    ----------------------    
    "finding modules files..."
    ${files.joinToString(separator = "\n").trimIndent()}
    ----------------------
    """.trimIndent()

    println(output)

    val moduleRoot = files
        .filter { !it.path.contains(".kt") }
        .firstOrNull {
            it.path.split("java").get(1).split("/").size == 4
        }

    output = """
    ----------------------    
    "Found modules root..."
    $moduleRoot
    ----------------------
    """.trimIndent()

    println(output)
    moduleRoot ?: return

    val basePath = "$moduleRoot/base"
    val baseFile = File(basePath)
    if (!baseFile.exists()) {
        output = """
    ----------------------    
    "created base folder..."
    ${baseFile.path}
    ----------------------
    """.trimIndent()
        println(output)
        baseFile.mkdir()
    }

    //val template = File("./src/main/kotlin/com/ch8n/viewbinder/utils/TemplateBaseViewBindingActivity.txt")
    val template =
        File("/Users/chetangupta/Documents/chetan/kotlin/ViewBindingMigrator/src/main/kotlin/com/ch8n/viewbinder/utils/TemplateBaseViewBindingActivity.txt")

    if (!template.exists()) {
        println("Activity Template not found!")
        return
    }

    var viewBindingActivityTemplate = template.readText(Charsets.UTF_8)
    val packageName = basePath.split("/").takeLast(4).joinToString(separator = ".")
    viewBindingActivityTemplate = "package ${packageName}\n\n${viewBindingActivityTemplate}"

    val viewBindingActivityKTPath = "$baseFile/ViewBindingActivity.kt"
    val viewBindingActivityKTFile = File(viewBindingActivityKTPath)
    if (!viewBindingActivityKTFile.exists()) {
        val output = """
    ------------------
    Wrote ViewBinding Activity to project
    ------------------
    $viewBindingActivityTemplate
    -----------------    
    """.trimIndent()

        println(output)

        viewBindingActivityKTFile.bufferedWriter().use { out ->
            out.write(viewBindingActivityTemplate)
        }
    }
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


    // TODO later add multiple module support
//    val selectedModules: List<String> =
//        KInquirer.promptListMultiObject("[space to select] : select module to install?", moduleName)
    val selectedModules = dependencyBuildFiles
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
                val (before: String, after: String) = content.split("buildFeatures {")
                val updatedContent: String = appendBuildFeatureGradle(before, after)
                println(updatedContent)
                oneFile.bufferedWriter().use { out ->
                    out.write(updatedContent)
                }
            }
        }
        else -> {
            val (before, after) = content.split("android {")
            val updatedContent: String = appendBuildFeatureViewBindingTemplate(before, after)
            println(updatedContent)
            oneFile.bufferedWriter().use { out ->
                out.write(updatedContent)
            }
        }
    }

}
