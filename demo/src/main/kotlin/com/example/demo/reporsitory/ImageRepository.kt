package com.example.demo.reporsitory

import com.example.demo.domain.Images
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ImageRepository : JpaRepository<Images, Long> {
    fun existsByImageName(imageName: String): Boolean

    @Query("SELECT COUNT(i) > 0 FROM Images i WHERE i.imageName = :imageName")
    fun imageExists(imageName: String): Boolean

    fun findByImageNameContainingIgnoreCase(name: String): List<Images>
}
