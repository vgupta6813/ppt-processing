package com.example.demo.standalone

import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xslf.usermodel.XSLFSlide
import java.awt.Dimension
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import javax.imageio.ImageIO


fun main3() {
    val pptFile = File("presentation1.pptx") // Specify your PPTX file
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
        val outputFile = File(outputFileName)

        // Save the image to a file
        ImageIO.write(img, "png", outputFile)

        println("Saved slide $slideNumber as ${outputFile.absolutePath}")
        slideNumber++
    }
}
