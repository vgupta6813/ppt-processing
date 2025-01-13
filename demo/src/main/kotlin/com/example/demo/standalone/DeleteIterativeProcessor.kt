package com.example.demo.standalone

import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xslf.usermodel.XSLFSlide
import java.awt.Dimension
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import javax.imageio.ImageIO

fun main1() {
    val folderPath = "/Users/vaishaligupta/Documents/presentations" // Specify the folder containing PPTX files
    val folder = File(folderPath)

    if (!folder.exists() || !folder.isDirectory) {
        println("Invalid folder path: $folderPath")
        return
    }

    // Get all PPTX files in the folder
    val pptFiles = folder.listFiles { file -> file.extension.lowercase() == "pptx" } ?: arrayOf()
    val pptFileNames = pptFiles.map { it.nameWithoutExtension }.toSet()

    // Step 1: Delete orphaned images
    println("Deleting orphaned images...")
    folder.listFiles { file -> file.extension.lowercase() == "png" }?.forEach { imageFile ->
        val baseName = imageFile.nameWithoutExtension // e.g., presentation1_1
        val pptName = baseName.substringBeforeLast('_') // Extract presentation name
        if (pptName !in pptFileNames) {
            // Delete the image if its related presentation is missing
            if (imageFile.delete()) {
                println("Deleted orphaned image: ${imageFile.name}")
            } else {
                println("Failed to delete image: ${imageFile.name}")
            }
        }
    }

    // Step 2: Process existing PPTX files and generate images
    println("Processing existing presentations...")
    for (pptFile in pptFiles) {
        println("Processing file: ${pptFile.name}")
        val pptFileName = pptFile.nameWithoutExtension // Extract the file name without extension
        val ppt = XMLSlideShow(FileInputStream(pptFile))

        val pageSize: Dimension = ppt.pageSize
        var slideNumber = 1

        for (slide: XSLFSlide in ppt.slides) {
            // Create an image with the slide dimensions
            val img = BufferedImage(pageSize.width, pageSize.height, BufferedImage.TYPE_INT_ARGB)
            val graphics = img.createGraphics()

            // Render the slide onto the graphics
            slide.draw(graphics)

            // Generate a unique name for the slide image
            val outputFileName = "${pptFileName}_$slideNumber.png"
            val outputFile = File(folder, outputFileName)

            // Save the image to a file
            ImageIO.write(img, "png", outputFile)

            println("Saved slide $slideNumber as ${outputFile.absolutePath}")
            slideNumber++
        }
    }

    println("Processing completed for all files in the folder.")
}
