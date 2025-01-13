package com.example.demo.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

data class PresentationInfo(val name: String, val path: String)

@RestController
@RequestMapping("/api/open-presentations")
class OpenPresentationsController {

    @GetMapping
    fun getOpenPresentations(): List<PresentationInfo> {
        val openPresentations = mutableListOf<PresentationInfo>()

        if (System.getProperty("os.name").lowercase().contains("win")) {
            openPresentations.addAll(getPowerPointOpenFilesWindows())
        } else if (System.getProperty("os.name").lowercase().contains("mac")) {
            openPresentations.addAll(getOpenPresentationsMac())
        } else {
            throw RuntimeException("Unsupported operating system: ${System.getProperty("os.name")}")
        }

        return openPresentations
    }

    private fun getPowerPointOpenFilesWindows(): List<PresentationInfo> {
        val presentations = mutableListOf<PresentationInfo>()

        com.sun.jna.platform.win32.User32.INSTANCE.EnumWindows(
            { hwnd, _ ->
                val titleLength = com.sun.jna.platform.win32.User32.INSTANCE.GetWindowTextLength(hwnd) + 1
                val title = CharArray(titleLength)
                com.sun.jna.platform.win32.User32.INSTANCE.GetWindowText(hwnd, title, titleLength)
                val windowTitle = String(title).trim()

                if (windowTitle.contains(".pptx", ignoreCase = true)) {
                    println("Detected PowerPoint window: $windowTitle")

                    // Extract file name from the title
                    val fileName = windowTitle.substringBeforeLast(".pptx").trim() + ".pptx"
                    val fullPath = searchFileSystemForPath(fileName)

                    if (fullPath != null) {
                        println("Resolved full path: $fullPath")
                        presentations.add(PresentationInfo(fileName, fullPath))
                    } else {
                        println("Could not resolve full path for: $fileName")
                    }
                }
                true
            },
            null
        )

        return presentations
    }

    private fun searchFileSystemForPath(fileName: String): String? {
        val commonFolders = listOf(
            System.getProperty("user.home") + "/Documents",
            System.getProperty("user.home") + "/Downloads",
            System.getProperty("user.home") + "/Desktop"
        )

        for (folder in commonFolders) {
            val file = File(folder, fileName)
            if (file.exists()) {
                return file.absolutePath
            }
        }

        return null
    }

    private fun getOpenPresentationsMac(): List<PresentationInfo> {
        val presentations = mutableListOf<PresentationInfo>()
        val processBuilder = ProcessBuilder("/bin/bash", "-c", "lsof | grep '.pptx'")
        val process = processBuilder.start()
        val lines = process.inputStream.bufferedReader().readLines()

        lines.forEach { line ->
            val parts = line.trim().split("\\s+".toRegex(), limit = 9)
            val filePath = parts.lastOrNull()
            if (filePath != null && Files.exists(Paths.get(filePath)) && filePath.endsWith(".pptx", true)) {
                val fileName = File(filePath).nameWithoutExtension
                presentations.add(PresentationInfo(fileName, filePath))
            }
        }

        return presentations
    }
}
