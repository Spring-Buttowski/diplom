import React from 'react';
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import MainPage from './MainPage';
import DiagramPage from './DiagramPage';

const App = () => {
    return (
        <Router>
            <Routes>
                <Route path="/" element={<MainPage />} />
                <Route path="/diagram/:boilerHouseName" element={<DiagramPage />} />
            </Routes>
        </Router>
    );
};

export default App;