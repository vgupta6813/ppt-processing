package com.example.demo.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "images")
data class Images(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "image_name", nullable = false, unique = true)
    val imageName: String,

    @Column(name = "image_path", nullable = false)
    val imagePath: String,

    @Column(name = "source_presentation_path", nullable = true)
    val sourcePresentationPath: String? = null,

    @Column(name = "source_slide_number", nullable = true)
    val sourceSlideNumber: Int? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    constructor() : this(
        id = 0,
        imageName = "",
        imagePath = "",
        createdAt = LocalDateTime.now()
    )
}
