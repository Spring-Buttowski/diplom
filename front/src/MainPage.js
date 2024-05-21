import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useDropzone } from 'react-dropzone';
import './App.css';

const MainPage = () => {
    const [boilerHouses, setBoilerHouses] = useState([]);
    const [newBoilerHouseName, setNewBoilerHouseName] = useState('');
    const [selectedFile, setSelectedFile] = useState(null);
    const [showSuccessMessage, setShowSuccessMessage] = useState(false);
    const [isLoading, setIsLoading] = useState(false);

    useEffect(() => {
        fetch('http://localhost:8080/boiler-houses')
            .then(response => response.json())
            .then(data => setBoilerHouses(data))
            .catch(error => console.error('Error fetching boiler houses:', error));
    }, []);

    const handleDelete = (name) => {
        fetch(`http://localhost:8080/boiler-houses/${name}`, {
            method: 'DELETE',
        })
            .then(() => setBoilerHouses(boilerHouses.filter(bh => bh.name !== name)))
            .catch(error => console.error('Error deleting boiler house:', error));
    };

    const onDrop = useCallback((acceptedFiles) => {
        setSelectedFile(acceptedFiles[0]);
    }, []);

    const handleSave = () => {
        if (!selectedFile || !newBoilerHouseName) {
            alert('Please select a file and enter a boiler house name.');
            return;
        }

        setIsLoading(true); // Show loading indicator

        const formData = new FormData();
        formData.append('file', selectedFile);
        formData.append('name', newBoilerHouseName);

        fetch('http://localhost:8080/create-boiler-house', {
            method: 'POST',
            body: formData,
        })
            .then(response => {
                setIsLoading(false); // Hide loading indicator
                if (response.status === 200) {
                    setShowSuccessMessage(true);
                    setTimeout(() => setShowSuccessMessage(false), 3000); // Hide after 3 seconds
                }
                return response.json();
            })
            .then(data => {
                setBoilerHouses([...boilerHouses, { name: newBoilerHouseName }]);
                setNewBoilerHouseName('');
                setSelectedFile(null);
            })
            .catch(error => {
                setIsLoading(false); // Hide loading indicator
                console.error('Error uploading file:', error);
            });
    };

    const { getRootProps, getInputProps, isDragActive } = useDropzone({ onDrop });

    return (
        <div className="container">
            <h1>Boiler Houses</h1>
            <ul>
                {boilerHouses.map(bh => (
                    <li key={bh.name}>
                        <Link to={`/diagram/${bh.name}`} className="boiler-house-link">
                            {bh.name}
                        </Link>
                        <button onClick={() => handleDelete(bh.name)}>Delete</button>
                    </li>
                ))}
            </ul>
            <div>
                <input
                    type="text"
                    value={newBoilerHouseName}
                    onChange={(e) => setNewBoilerHouseName(e.target.value)}
                    placeholder="New Boiler House Name"
                />
                <div {...getRootProps()} className="dropzone">
                    <input {...getInputProps()} />
                    {isDragActive ? (
                        <p>Drop the files here ...</p>
                    ) : (
                        <p>Drag 'n' drop some files here, or click to select files</p>
                    )}
                    {selectedFile && <p>Selected file: {selectedFile.name}</p>}
                </div>
                <button className="save" onClick={handleSave}>Save</button>
            </div>
            {isLoading && (
                <div className="loading-indicator">
                    Loading...
                </div>
            )}
            {showSuccessMessage && (
                <div className="success-message">
                    File was loaded successfully!
                </div>
            )}
        </div>
    );
};

export default MainPage;