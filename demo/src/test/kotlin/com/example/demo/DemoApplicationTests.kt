package com.example.demo

import com.jacob.activeX.ActiveXComponent


object JacobTest {
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            System.load("C:\\libs\\jacob-1.21\\jacob-1.21-x64.dll")
            val powerPointApp = ActiveXComponent("PowerPoint.Application")
            val presentations = powerPointApp.getProperty("Presentations").toDispatch()
            println("PowerPoint is running.")
            powerPointApp.safeRelease()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

