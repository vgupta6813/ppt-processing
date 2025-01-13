package com.example.demo.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "image_tags")
data class ImageTags(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne
    @JoinColumn(name = "image_id", nullable = false)
    val image: Images,

    @ManyToOne
    @JoinColumn(name = "tag_id", nullable = false)
    val tag: Tags
) {
    // No-argument constructor for JPA
    constructor() : this(
        id = 0,
        image = Images(),
        tag = Tags()
    )
}