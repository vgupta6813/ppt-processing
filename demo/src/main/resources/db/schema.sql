-- Updated Table to store images (without binary data)
CREATE TABLE images (
    id SERIAL PRIMARY KEY,                -- Unique identifier for each image
    image_name VARCHAR(255) NOT NULL,     -- Name of the image
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- Timestamp for image upload
);

-- Table to store unique tags
CREATE TABLE tags (
    id SERIAL PRIMARY KEY,                -- Unique identifier for each tag
    tag_name VARCHAR(255) NOT NULL UNIQUE -- Tag name
);

-- Table to link images and tags (many-to-many)
CREATE TABLE image_tags (
    image_id INT NOT NULL,                -- Foreign key to images table
    tag_id INT NOT NULL,                  -- Foreign key to tags table
    PRIMARY KEY (image_id, tag_id),       -- Composite primary key
    FOREIGN KEY (image_id) REFERENCES images (id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE
);
