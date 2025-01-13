package com.example.demo.reporsitory

import com.example.demo.domain.Tags
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TagRepository : JpaRepository<Tags, Long> {
    fun findByTagName(tagName: String): Optional<Tags>
}
