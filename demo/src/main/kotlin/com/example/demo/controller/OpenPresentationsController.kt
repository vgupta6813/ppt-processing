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
        println("inside getOpenPresentations")
        val openPresentations = mutableListOf<PresentationInfo>()

        if (System.getProperty("os.name").lowercase().contains("win")) {
            println("vaishali OS is windows")
            openPresentations.addAll(getPowerPointOpenFilesWindows())
        } else if (System.getProperty("os.name").lowercase().contains("mac")) {
            openPresentations.addAll(getOpenPresentationsMac())
        } else {
            throw RuntimeException("Unsupported operating system: ${System.getProperty("os.name")}")
        }

        return openPresentations
    }

    private fun getPowerPointOpenFilesWindows(): List<PresentationInfo> {
        println("inside getPowerPointOpenFilesWindows")
        val presentations = mutableListOf<PresentationInfo>()

        // Check if there are open PowerPoint windows
        println("vaishali Checking if there are open PowerPoint windows")
        com.sun.jna.platform.win32.User32.INSTANCE.EnumWindows(
            { hwnd, _ ->
                val titleLength = com.sun.jna.platform.win32.User32.INSTANCE.GetWindowTextLength(hwnd) + 1
                val title = CharArray(titleLength)
                com.sun.jna.platform.win32.User32.INSTANCE.GetWindowText(hwnd, title, titleLength)
                val windowTitle = String(title).trim()

                if (windowTitle.contains(".pptx", ignoreCase = true)) {
                    // Attempt to retrieve the file path from the window title
                    val fileName = windowTitle.substringBeforeLast(".pptx").trim() + ".pptx"
                    val fullPath = searchFileSystemForPath(fileName)

                    if (fullPath != null) {
                        println("vaishali ==== fullPath $fullPath")
                        presentations.add(PresentationInfo(fileName, fullPath))
                    }
                }
                true
            },
            null
        )

        return presentations
    }

    private fun getOpenPresentationsMac(): List<PresentationInfo> {
        val presentations = mutableListOf<PresentationInfo>()
        val processBuilder = ProcessBuilder("/bin/bash", "-c", "lsof | grep '.pptx'")
        val process = processBuilder.start()
        val lines = process.inputStream.bufferedReader().readLines()

        lines.forEach { line ->
            val parts = line.trim().split("\\s+".toRegex(), limit = 9)
            val filePath = parts.lastOrNull()
            if (filePath != null && filePath.endsWith(".pptx", true)) {
                val fileName = File(filePath).nameWithoutExtension
                presentations.add(PresentationInfo(fileName, filePath))
            }
        }

        return presentations
    }

    private fun searchFileSystemForPath(fileName: String): String? {
        println("inside searchFileSystemForPath")
        val commonFolders = listOf(
            System.getProperty("user.home") + "/Documents",
            System.getProperty("user.home") + "/Downloads",
            System.getProperty("user.home") + "/Desktop",
            System.getProperty("user.home") + "/OneDrive/*",
            System.getProperty("user.home") + "/OneDrive/Desktop",
            System.getProperty("user.home") + "/OneDrive/Downloads",
            System.getProperty("user.home") + "/OneDrive/Documents/open_presentations_one_drive",
            System.getProperty("user.home") + "/OneDrive/Documents/"
        )

        println("vaishali common folder $commonFolders")

        for (folder in commonFolders) {
            println("folder in commonFolders")
            val file = File(folder, fileName)
            val flag = file.exists()
            println("vaishali file exists in folder -> $flag")
            if (flag) {
                val a = file.absolutePath
                println("vaishali file path $a")
                return a
            }
        }

        return null
    }
}
