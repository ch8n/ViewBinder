package com.ch8n.viewbinder


import com.ch8n.viewbinder.utils.appendBuildFeatureViewBindingTemplate
import com.ch8n.viewbinder.utils.appendBuildFeatureGradle
import com.yg.kotlin.inquirer.components.promptConfirm
import com.yg.kotlin.inquirer.components.promptInput
import com.yg.kotlin.inquirer.core.Choice
import com.yg.kotlin.inquirer.core.KInquirer
import java.io.File


object Config {
    val projectRoot: String = "/Users/chetangupta/StudioProjects/ColorChetan"
    val gradleEXT: String = ".gradle"
}


fun main() {

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

    updateSuperClassToViewBind(activities)

}

fun updateSuperClassToViewBind(activities: List<File>) {
    // todo solve for multiple class
    val activity = activities.get(0)
    val content = activity.readText(Charsets.UTF_8)
    // todo
    // 1. add to import -> import com.example.colorapp.base.ViewBindingActivity
    // 2.replace AppCompatActivity => ViewBindingActivity<VB>
    // 3.find line that contain -> R.layout.activity_main
    // 4. create viewbinding name of layout => activity_main -> ActivityMainBinding
    // 5. replace VB to viewbindingName
    // 6. add import of viewbinding >> import com.example.colorapp.databinding.ActivityMainBinding
    // find package name
    // append naming convention
    // 7. add functions
    // 1. override val bindingInflater: (LayoutInflater) -> ActivityMainBinding get() = ActivityMainBinding::inflate
    // 2. add setup()
    // paste all the code from onCreate to step except line
    // super.onCreate(savedInstanceState)
    // setContentView(R.layout.activity_main)


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
