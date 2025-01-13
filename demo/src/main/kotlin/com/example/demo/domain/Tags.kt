package com.example.demo.domain

import jakarta.persistence.*

@Entity
@Table(name = "tags")
data class Tags(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "tag_name", nullable = false, unique = true)
    val tagName: String
) {
    // No-argument constructor for JPA
    constructor() : this(
        id = 0,
        tagName = ""
    )
}

