package com.example.demo.controller

import com.example.demo.reporsitory.ImageRepository
import com.jacob.activeX.ActiveXComponent
import com.jacob.com.Dispatch
import org.springframework.web.bind.annotation.*
import java.io.File

@RestController
@RequestMapping("/api/presentations")
class PresentationController(
    val imageRepository: ImageRepository
) {

    @PostMapping("/add-slides")
    fun addSlidesToOpenPresentation(
        @RequestParam folderPath: String,
        @RequestBody imageIds: List<Long>
    ): String {
        val os = System.getProperty("os.name").lowercase()

        return if (os.contains("win")) {
            addSlidesToOpenPresentationWindows(folderPath, imageIds)
        } else if (os.contains("mac")) {
            addSlidesToOpenPresentationMac(folderPath, imageIds)
        } else {
            throw RuntimeException("Unsupported operating system: $os")
        }
    }

    private fun addSlidesToOpenPresentationWindows(folderPath: String, imageIds: List<Long>): String {
        println("folder path $folderPath")
        // Normalizing the file path of the selected presentation
        val normalizedFolderPath = folderPath.replace("\\", "/")
        val powerPointApp = ActiveXComponent("PowerPoint.Application")
        val presentations = Dispatch.get(powerPointApp, "Presentations").toDispatch()

        var targetPresentation: Dispatch? = null
        val presentationsCount = Dispatch.get(presentations, "Count").toInt()

        // Loop over open presentations to find the matching presentation
        for (i in 1..presentationsCount) {
            val presentation = Dispatch.call(presentations, "Item", i).toDispatch()
            val path = Dispatch.get(presentation, "FullName").toString().replace("\\", "/")  // Normalize path

            // Print paths for debugging
            println("Checking presentation at path: $path")
            println("Looking for presentation with path: $normalizedFolderPath")

            if (path == normalizedFolderPath) {
                println("Found matching presentation: $path")
                targetPresentation = presentation
                break
            }
        }

        if (targetPresentation == null) {
            throw RuntimeException("The specified presentation is not open.")
        }

        // Proceed with the logic to add slides to the found presentation
        imageIds.forEach { imageId ->
            val image = imageRepository.findById(imageId).orElseThrow {
                RuntimeException("Image not found for ID: $imageId")
            }

            val sourcePath = image.sourcePresentationPath
                ?: throw RuntimeException("Source presentation path missing for image: ${image.imageName}")
            val slideNumber = image.sourceSlideNumber
                ?: throw RuntimeException("Source slide number missing for image: ${image.imageName}")

            val sourcePresentation = Dispatch.call(presentations, "Open", sourcePath, true, false, false).toDispatch()

            val slides = Dispatch.get(sourcePresentation, "Slides").toDispatch()
            val slideToCopy = Dispatch.call(slides, "Item", slideNumber).toDispatch()
            Dispatch.call(slideToCopy, "Copy")

            val targetSlides = Dispatch.get(targetPresentation, "Slides").toDispatch()
            Dispatch.call(targetSlides, "Paste")

            Dispatch.call(sourcePresentation, "Close")
        }

        Dispatch.call(targetPresentation, "Save")
//        Dispatch.call(targetPresentation, "Close")
        powerPointApp.safeRelease()

        return "Slides added to presentation on Windows."
    }

    private fun getOpenPresentationByPath(filePath: String, presentations: Dispatch): Dispatch? {
        // Log the file path we are searching for
        println("Looking for presentation with path: $filePath")

        // Loop through the open presentations and find the one matching the file path
        val presentationsCount = Dispatch.get(presentations, "Count").toInt()
        for (i in 1..presentationsCount) {
            val presentation = Dispatch.call(presentations, "Item", i).toDispatch()
            val path = Dispatch.get(presentation, "FullName").toString()

            // Log the path of the currently open presentation
            println("Checking presentation at path: $path")

            if (path.equals(filePath, ignoreCase = true)) {
                println("Found matching presentation: $path")
                return presentation
            }
        }

        println("Presentation not found for path: $filePath")
        return null // If not found
    }

    // Mac implementation
    private fun addSlidesToOpenPresentationMac(folderPath: String, imageIds: List<Long>): String {
        val slidesScript = StringBuilder()
        slidesScript.appendLine("tell application \"Microsoft PowerPoint\"")
        slidesScript.appendLine("set activePresentation to open POSIX file \"$folderPath\"")

        imageIds.forEach { imageId ->
            val image = imageRepository.findById(imageId).orElseThrow {
                RuntimeException("Image not found for ID: $imageId")
            }

            val sourcePath = image.sourcePresentationPath ?: throw RuntimeException("Source presentation path missing for image: ${image.imageName}")
            val slideNumber = image.sourceSlideNumber ?: throw RuntimeException("Source slide number missing for image: ${image.imageName}")

            slidesScript.appendLine("try")
            slidesScript.appendLine("set sourceFilePath to POSIX file \"$sourcePath\"")
            slidesScript.appendLine("set sourcePresentation to null")

            // Check if the presentation is already open
            slidesScript.appendLine("repeat with pres in presentations")
            slidesScript.appendLine("if pres's full name contains \"$sourcePath\" then")
            slidesScript.appendLine("set sourcePresentation to pres")
            slidesScript.appendLine("exit repeat")
            slidesScript.appendLine("end if")
            slidesScript.appendLine("end repeat")

            // Open the presentation if it's not already open
            slidesScript.appendLine("if sourcePresentation is null then")
            slidesScript.appendLine("set sourcePresentation to open sourceFilePath")
            slidesScript.appendLine("end if")

            // Validate source presentation and slide
            slidesScript.appendLine("if sourcePresentation is not missing value then")
            slidesScript.appendLine("set slideCount to count slides of sourcePresentation")
            slidesScript.appendLine("if $slideNumber ≤ slideCount then")
            slidesScript.appendLine("set sourceSlide to slide $slideNumber of sourcePresentation")
            slidesScript.appendLine("if sourceSlide is not missing value then")
            slidesScript.appendLine("duplicate sourceSlide to end of slides of activePresentation")
            slidesScript.appendLine("else")
            slidesScript.appendLine("log \"Error: Slide $slideNumber does not exist in $sourcePath\"")
            slidesScript.appendLine("end if")
            slidesScript.appendLine("else")
            slidesScript.appendLine("log \"Error: Slide number $slideNumber exceeds slide count in $sourcePath\"")
            slidesScript.appendLine("end if")
            slidesScript.appendLine("else")
            slidesScript.appendLine("log \"Error: Could not open source presentation $sourcePath\"")
            slidesScript.appendLine("end if")

            slidesScript.appendLine("close sourcePresentation saving no")
            slidesScript.appendLine("on error errMsg")
            slidesScript.appendLine("log \"Error: \" & errMsg")
            slidesScript.appendLine("end try")
        }

        slidesScript.appendLine("save activePresentation")
        slidesScript.appendLine("close activePresentation")
        slidesScript.appendLine("end tell")

        executeAppleScript(slidesScript.toString())

        return "Slides added to presentation on macOS."
    }

    private fun executeAppleScript(script: String) {
        println("Generated AppleScript:\n$script")

        val processBuilder = ProcessBuilder("/usr/bin/osascript", "-e", script)
        val process = processBuilder.start()
        val output = process.inputStream.bufferedReader().readText()
        val error = process.errorStream.bufferedReader().readText()

        if (error.isNotEmpty()) {
            throw RuntimeException("AppleScript error: $error")
        }

        println("AppleScript output: $output")
    }

    @GetMapping
    fun getPresentations(@RequestParam folderPath: String): List<String> {
        val folder = File(folderPath)
        if (!folder.exists() || !folder.isDirectory) {
            throw RuntimeException("Invalid folder path: $folderPath")
        }
        return folder.listFiles { file -> file.extension.lowercase() == "pptx" }
            ?.map { it.nameWithoutExtension }
            ?: emptyList()
    }
}
