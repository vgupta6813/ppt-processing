package com.example.demo.controller

import com.example.demo.domain.Images
import com.example.demo.reporsitory.ImageRepository
import org.apache.poi.sl.usermodel.PictureData.PictureType
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.imageio.ImageIO
import com.jacob.activeX.ActiveXComponent
import com.jacob.com.Dispatch
import com.jacob.com.Variant

@RestController
@RequestMapping("/api/presentations")
class PresentationController(
    val imageRepository: ImageRepository
) {
//    @PostMapping("/add-slides")
//    fun addImagesToPresentation(
//        @RequestParam folderPath: String,
//        @RequestBody imagePaths: List<String>
//    ): String {
//        println("Received folderPath: $folderPath")
//        println("Received imagePaths: $imagePaths")
//
//        val presentationFile = File(folderPath)
//        if (!presentationFile.exists()) {
//            throw RuntimeException("Presentation not found: $folderPath")
//        }
//
//        println("presentation path -> ${presentationFile.path}")
//
//        val ppt = XMLSlideShow(FileInputStream(presentationFile))
//        val pageSize = ppt.pageSize // Dimensions of the presentation slides
//
//        println("page size -> ${pageSize.size}")
//
//        imagePaths.forEach { imagePath ->
//            val slide = ppt.createSlide()
//            val image = File(imagePath)
//
//            println("slide number ${slide.slideNumber}")
//            println("image name ${image.name}")
//
//            if (!image.exists()) {
//                throw RuntimeException("Image not found: $imagePath")
//            }
//
//            try {
//                println("inside try block")
//                // Add the image to the slide
//                val pictureData = ppt.addPicture(image, PictureType.PNG)
//                val picture = slide.createPicture(pictureData)
//
//                // Set the picture dimensions to match the slide size
//                picture.anchor = java.awt.Rectangle(0, 0, pageSize.width, pageSize.height)
//            } catch (e: Exception) {
//                println("Error adding image to slide: ${e.message}")
//            }
//        }
//
//        println("outside for loop")
//        // Save the updated presentation
//        FileOutputStream(presentationFile).use { fos ->
//            ppt.write(fos)
//        }
//
//        val response = "Images added to presentation: $folderPath"
//
//        println("response $response")
//
//        return response
//    }

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
        val powerPointApp = ActiveXComponent("PowerPoint.Application")
        val presentations = Dispatch.get(powerPointApp, "Presentations").toDispatch()

        val targetPresentation = Dispatch.call(
            presentations,
            "Open",
            folderPath,
            true, // ReadOnly
            false, // Untitled
            false // WithWindow
        ).toDispatch()

        imageIds.forEach { imageId ->
            val image = imageRepository.findById(imageId).orElseThrow {
                RuntimeException("Image not found for ID: $imageId")
            }

            val sourcePath = image.sourcePresentationPath
                ?: throw RuntimeException("Source presentation path missing for image: ${image.imageName}")
            val slideNumber = image.sourceSlideNumber
                ?: throw RuntimeException("Source slide number missing for image: ${image.imageName}")

            val sourcePresentation = Dispatch.call(
                presentations,
                "Open",
                sourcePath,
                true, // ReadOnly
                false, // Untitled
                false // WithWindow
            ).toDispatch()

            val slides = Dispatch.get(sourcePresentation, "Slides").toDispatch()
            val slideToCopy = Dispatch.call(slides, "Item", slideNumber).toDispatch()
            Dispatch.call(slideToCopy, "Copy")

            val targetSlides = Dispatch.get(targetPresentation, "Slides").toDispatch()
            Dispatch.call(targetSlides, "Paste")

            Dispatch.call(sourcePresentation, "Close")
        }

        Dispatch.call(targetPresentation, "Save")
        Dispatch.call(targetPresentation, "Close")
        powerPointApp.safeRelease()

        return "Slides added to presentation on Windows."
    }

    private fun addSlidesToOpenPresentationMac(folderPath: String, imageIds: List<Long>): String {
        val slidesScript = StringBuilder()
        slidesScript.appendLine("tell application \"Microsoft PowerPoint\"")
        slidesScript.appendLine("set activePresentation to open POSIX file \"$folderPath\"")

        imageIds.forEach { imageId ->
            val image = imageRepository.findById(imageId).orElseThrow {
                RuntimeException("Image not found for ID: $imageId")
            }

            val sourcePath = image.sourcePresentationPath
                ?: throw RuntimeException("Source presentation path missing for image: ${image.imageName}")
            val slideNumber = image.sourceSlideNumber
                ?: throw RuntimeException("Source slide number missing for image: ${image.imageName}")

            println("sourcePath -> $sourcePath")
            println("slideNumber -> $slideNumber")

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

        println("Generated AppleScript:\n$slidesScript")

        executeAppleScript(slidesScript.toString())

        return "Slides added to presentation on macOS."
    }

    private fun executeAppleScript(script: String) {
        println("inside executeAppleScript Generated AppleScript:\n$script")

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
