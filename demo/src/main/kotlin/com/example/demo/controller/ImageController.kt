package com.example.demo.controller

import com.example.demo.domain.ImageTags
import com.example.demo.domain.Images
import com.example.demo.domain.Tags
import com.example.demo.reporsitory.ImageRepository
import com.example.demo.reporsitory.ImageTagRepository
import com.example.demo.reporsitory.TagRepository
import org.apache.poi.sl.usermodel.PictureData.PictureType
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xslf.usermodel.XSLFSlide
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.awt.Dimension
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Paths
import javax.imageio.ImageIO


@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = ["http://localhost:3000"]) // Allow React app's origin
class ImageController(
    private val imageRepository: ImageRepository,
    private val tagRepository: TagRepository,
    private val imageTagRepository: ImageTagRepository
) {
    @Transactional
    @PostMapping("/process")
    fun processPresentations(@RequestParam folderPath: String): String {
        println("inside processPresentations")

        val folder = File(folderPath)

        if (!folder.exists() || !folder.isDirectory) {
            return "Invalid folder path: $folderPath"
        }

        val pptFiles = folder.listFiles { file -> file.extension.lowercase() == "pptx" } ?: arrayOf()
        val pptFileNames = pptFiles.map { it.nameWithoutExtension }.toSet()

        // Step 1: Delete orphan images and their tags
        val imagesInDb = imageRepository.findAll()
        imagesInDb.forEach { image ->
            val baseName = image.imageName.substringBeforeLast('_')
            if (baseName !in pptFileNames) {
                // Delete file and associated database records
                val imageFile = File(folder, image.imageName)
                if (imageFile.exists() && imageFile.delete()) {
                    println("Deleted orphan image file: ${imageFile.absolutePath}")
                }
                val imageTags = imageTagRepository.findAllByImageId(image.id)
                imageTags.forEach { imageTag ->
                    val tag = imageTag.tag
                    imageTagRepository.delete(imageTag)
                    if (!imageTagRepository.existsByTagId(tag.id)) {
                        tagRepository.delete(tag)
                    }
                }
                imageRepository.delete(image)
            }
        }

        // Step 2: Process and save new images
        for (pptFile in pptFiles) {
            val ppt = XMLSlideShow(FileInputStream(pptFile))
            val pageSize = ppt.pageSize

            var slideNumber = 1
            for (slide in ppt.slides) {
                println("slide in ppt slide ${slide}")
                val imageName = "${pptFile.nameWithoutExtension}_$slideNumber.png"
                val imageFile = File(folder, imageName)

                // Check if the image already exists in the database
                if (imageRepository.existsByImageName(imageName)) {
                    println("Skipping $imageName, already exists in the database")
                    slideNumber++
                    continue
                } else {
                    println("Inserting image: $imageName")
                }


                // Generate image
                val img = BufferedImage(pageSize.width, pageSize.height, BufferedImage.TYPE_INT_ARGB)
                val graphics = img.createGraphics()
                slide.draw(graphics)
                ImageIO.write(img, "png", imageFile)

                // Save metadata to the database
                val image = Images(
                    imageName = imageName,
                    imagePath = imageFile.absolutePath,
                    sourcePresentationPath = pptFile.absolutePath, // Added field
                    sourceSlideNumber = slideNumber // Added field
                )
                imageRepository.saveAndFlush(image)

                slideNumber++
            }
        }

        return "Images processed and saved."
    }

    // Add tags to an existing image
    @PostMapping("/{id}/tags")
    fun addTags(@PathVariable id: Long, @RequestBody tags: List<String>): String {
        val image = imageRepository.findById(id).orElseThrow { RuntimeException("Image not found") }

        tags.forEach { tagName ->
            val tag = tagRepository.findByTagName(tagName).orElseGet {
                tagRepository.save(Tags(tagName = tagName))
            }

            imageTagRepository.save(ImageTags(image = image, tag = tag))
        }

        return "Tags added successfully to image: ${image.imageName}"
    }

    // Retrieve tags for an image
    @GetMapping("/{id}/tags")
    fun getTags(@PathVariable id: Long): List<String> {
        val imageTags = imageTagRepository.findAll().filter { it.image.id == id }
        return imageTags.map { it.tag.tagName }.distinct()
    }

    @GetMapping
    fun getAllImages(): List<Images> {
        println("inside getAllImages")
        return imageRepository.findAll() // Replace with actual repository call
    }

    @GetMapping("/{id}")
    fun getImage(@PathVariable id: Long): ResponseEntity<Resource> {
        val image = imageRepository.findById(id).orElseThrow { RuntimeException("Image not found") }
        val imagePath = Paths.get(image.imagePath)

        val resource: Resource = UrlResource(imagePath.toUri())
        return if (resource.exists() || resource.isReadable) {
            ResponseEntity.ok(resource)
        } else {
            throw RuntimeException("Could not read the file!")
        }
    }

    @GetMapping("/tags/{tagName}")
    fun getImagesByTag(@PathVariable tagName: String): List<Images> {
        println("Searching for images with tag: $tagName")

        val tag = tagRepository.findByTagName(tagName).orElseThrow {
            println("Tag not found: $tagName")
            RuntimeException("Tag not found")
        }
        println("Found tag: ${tag.tagName}")

        val imageTags = imageTagRepository.findAllByTagId(tag.id)
        println("Image-Tag associations found: ${imageTags.size}")

        val images = imageTags.map { it.image }.distinct()
        println("Images found for tag $tagName: ${images.size}")

        return images
    }

    @GetMapping("/search")
    fun searchImagesByName(@RequestParam name: String): List<Images> {
        return imageRepository.findByImageNameContainingIgnoreCase(name)
    }

    @DeleteMapping("/{id}/tags")
    fun removeTags(@PathVariable id: Long): String {
        val image = imageRepository.findById(id).orElseThrow { RuntimeException("Image not found") }
        val imageTags = imageTagRepository.findAllByImageId(image.id)

        imageTags.forEach { imageTag ->
            imageTagRepository.delete(imageTag)
            println("Removed tag association: ${imageTag.tag.tagName}")
        }

        return "Tags removed successfully for image: ${image.imageName}"
    }
}
