package com.example.demo.standalone

import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xslf.usermodel.XSLFSlide
import java.awt.Dimension
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import javax.imageio.ImageIO

fun main2() {
    val folderPath = "/Users/vaishaligupta/Documents/presentations" // Specify the folder containing PPTX files
    val folder = File(folderPath)

    if (!folder.exists() || !folder.isDirectory) {
        println("Invalid folder path: $folderPath")
        return
    }

    // Get all PPTX files in the folder
    val pptFiles = folder.listFiles { file -> file.extension.lowercase() == "pptx" } ?: arrayOf()

    if (pptFiles.isEmpty()) {
        println("No PPTX files found in the folder: $folderPath")
        return
    }

    // Process each PPTX file
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
