package com.example.demo.reporsitory

import com.example.demo.domain.ImageTags
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ImageTagRepository : JpaRepository<ImageTags, Long> {
    fun findAllByImageId(imageId: Long): List<ImageTags>
    fun existsByTagId(tagId: Long): Boolean
    fun findAllByTagId(tagId: Long): List<ImageTags>
}
