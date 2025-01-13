import React, { useState, useEffect } from "react";
import axios from "axios";

function App() {
    const [images, setImages] = useState([]);
    const [selectedImages, setSelectedImages] = useState([]);
    const [tag, setTag] = useState("");
    const [searchTag, setSearchTag] = useState("");
    const [searchSlideName, setSearchSlideName] = useState("");
    const [activePresentations, setActivePresentations] = useState([]);
    const [selectedPresentation, setSelectedPresentation] = useState("");
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        processPresentations(); // Process PowerPoint presentations to generate images
        loadImages(); // Load all images
        loadActivePresentations(); // Load open presentations
    }, []);

    // Process PowerPoint presentations to generate images
    const processPresentations = async () => {
        try {
            const folderPath = "/Users/vaishaligupta/Documents/presentations/"; // Replace with actual folder path
            await axios.post("http://localhost:8080/api/images/process", null, {
                params: { folderPath },
            });
            console.log("Presentations processed successfully.");
        } catch (error) {
            console.error("Error processing presentations:", error);
        }
    };

    // Load all images
    const loadImages = async () => {
        try {
            const response = await axios.get("http://localhost:8080/api/images");
            setImages(response.data);
        } catch (error) {
            console.error("Error loading images:", error);
        } finally {
            setLoading(false);
        }
    };

    // Load open PowerPoint presentations
    const loadActivePresentations = async () => {
        try {
            const response = await axios.get("http://localhost:8080/api/open-presentations");
            console.log("Active presentations fetched:", response.data);
            setActivePresentations(response.data);
        } catch (error) {
            console.error("Error loading open presentations:", error);
        }
    };

    // Select/Deselect an image
    const handleSelectImage = (id) => {
        setSelectedImages((prev) =>
            prev.includes(id) ? prev.filter((imageId) => imageId !== id) : [...prev, id]
        );
    };

    // Add a tag to selected images
    const handleAddTag = async () => {
        try {
            if (!tag) {
                alert("Please enter a tag!");
                return;
            }
            for (const imageId of selectedImages) {
                await axios.post(`http://localhost:8080/api/images/${imageId}/tags`, [tag]);
            }
            alert("Tag added successfully!");
            setSelectedImages([]);
            setTag("");
        } catch (error) {
            console.error("Error adding tag:", error);
        }
    };

    // Remove tags from selected images
    const handleRemoveTag = async () => {
        try {
            for (const imageId of selectedImages) {
                await axios.delete(`http://localhost:8080/api/images/${imageId}/tags`);
            }
            alert("Tags removed successfully!");
            setSelectedImages([]);
            loadImages();
        } catch (error) {
            console.error("Error removing tags:", error);
        }
    };

    // Search by tag
    const handleSearchByTag = async () => {
        try {
            const response = await axios.get(
                `http://localhost:8080/api/images/tags/${searchTag}`
            );
            setImages(response.data);
        } catch (error) {
            console.error("Error searching images by tag:", error);
        }
    };

    // Search by slide name
    const handleSearchBySlideName = async () => {
        try {
            const response = await axios.get(
                `http://localhost:8080/api/images/search?name=${searchSlideName}`
            );
            setImages(response.data);
        } catch (error) {
            console.error("Error searching images by slide name:", error);
        }
    };

    // Add selected images as slides to the selected presentation
    const handleAddToPresentation = async () => {
        if (!selectedPresentation) {
            alert("Please select a presentation!");
            return;
        }

        const selectedImageIds = images
            .filter((image) => selectedImages.includes(image.id))
            .map((image) => image.id);

        try {
            await axios.post(
                `http://localhost:8080/api/presentations/add-slides`,
                selectedImageIds,
                {
                    params: { folderPath: selectedPresentation },
                }
            );
            alert("Slides added successfully!");
            setSelectedImages([]);
        } catch (error) {
            console.error("Error adding slides to presentation:", error);
        }
    };

    // Reset all searches and show all images
    const handleReset = async () => {
        setSearchTag("");
        setSearchSlideName("");
        await loadImages();
    };

    return (
        <div className="App container">
            <h1>Image Tagging and Presentation Editing</h1>

            {/* Search by Tag */}
            <div className="mb-4">
                <input
                    type="text"
                    className="form-control"
                    placeholder="Search by tag"
                    value={searchTag}
                    onChange={(e) => setSearchTag(e.target.value)}
                />
                <button className="btn btn-primary mt-2" onClick={handleSearchByTag}>
                    Search by Tag
                </button>
            </div>

            {/* Search by Slide Name */}
            <div className="mb-4">
                <input
                    type="text"
                    className="form-control"
                    placeholder="Search by slide name"
                    value={searchSlideName}
                    onChange={(e) => setSearchSlideName(e.target.value)}
                />
                <button className="btn btn-secondary mt-2" onClick={handleSearchBySlideName}>
                    Search by Slide Name
                </button>
            </div>

            {/* Reset */}
            <div className="mb-4">
                <button className="btn btn-warning" onClick={handleReset}>
                    Reset Search
                </button>
            </div>

            {/* Add and Remove Tags */}
            <div className="mb-4">
                <input
                    type="text"
                    className="form-control"
                    placeholder="Enter tag to add"
                    value={tag}
                    onChange={(e) => setTag(e.target.value)}
                />
                <button className="btn btn-success mt-2" onClick={handleAddTag}>
                    Add Tag to Selected Images
                </button>
                <button className="btn btn-danger mt-2" onClick={handleRemoveTag}>
                    Remove Tags from Selected Images
                </button>
            </div>

            {/* Active Presentations Dropdown */}
            <div className="mb-4">
                <label htmlFor="activePresentations">Select Presentation:</label>
                <select
                    id="activePresentations"
                    className="form-select"
                    value={selectedPresentation}
                    onChange={(e) => setSelectedPresentation(e.target.value)}
                >
                    <option value="">-- Select a Presentation --</option>
                    {activePresentations.map((presentation, index) => (
                        <option key={index} value={presentation.path}>
                            {presentation.name}
                        </option>
                    ))}
                </select>
            </div>

            {/* Add to Presentation */}
            <div className="mb-4">
                <button
                    className="btn btn-primary"
                    onClick={handleAddToPresentation}
                    disabled={!selectedPresentation || selectedImages.length === 0}
                >
                    Add Selected Images to Presentation
                </button>
            </div>

            {/* Images */}
            {loading ? (
                <p>Loading images, please wait...</p>
            ) : (
                <div className="row">
                    {images.map((image) => (
                        <div className="col-md-4" key={image.id}>
                            <div className="card mb-4">
                                <input
                                    type="checkbox"
                                    className="form-check-input"
                                    onChange={() => handleSelectImage(image.id)}
                                    checked={selectedImages.includes(image.id)}
                                />
                                <img
                                    src={`http://localhost:8080/api/images/${image.id}`}
                                    alt={image.imageName}
                                    className="card-img-top"
                                />
                                <div className="card-body">
                                    <h5 className="card-title">{image.imageName}</h5>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}

export default App;
