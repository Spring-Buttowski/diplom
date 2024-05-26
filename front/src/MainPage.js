import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useDropzone } from 'react-dropzone';
import './App.css';

const MainPage = () => {
    const [boilerHouses, setBoilerHouses] = useState([]);
    const [newBoilerHouseName, setNewBoilerHouseName] = useState('');
    const [selectedDataFile, setSelectedDataFile] = useState(null);
    const [selectedParamsFile, setSelectedParamsFile] = useState(null);
    const [showSuccessMessage, setShowSuccessMessage] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [idealParameters, setIdealParameters] = useState({});
    const [dimensions, setDimensions] = useState({});
    const [numberOfBurners, setNumberOfBurners] = useState(0);
    const [burnerIndices, setBurnerIndices] = useState({});

    useEffect(() => {
        fetch('http://localhost:8080/boiler-houses')
            .then(response => response.json())
            .then(data => setBoilerHouses(data))
            .catch(error => console.error('Ошибка при получении данных котельных установок:', error));
    }, []);

    const handleDelete = (name) => {
        fetch(`http://localhost:8080/boiler-houses/${name}`, {
            method: 'DELETE',
        })
            .then(() => setBoilerHouses(boilerHouses.filter(bh => bh.name !== name)))
            .catch(error => console.error('Ошибка при удалении котельной установки:', error));
    };

    const onDropData = useCallback((acceptedFiles) => {
        setSelectedDataFile(acceptedFiles[0]);
    }, []);

    const onDropParams = useCallback((acceptedFiles) => {
        const file = acceptedFiles[0];
        setSelectedParamsFile(file);

        const reader = new FileReader();
        reader.onload = (event) => {
            const json = JSON.parse(event.target.result);
            setNewBoilerHouseName(json.boilerHouseName);
            setNumberOfBurners(json.numberOfBurners);

            const burners = json.burners;
            const newIdealParameters = {};
            const newDimensions = {};
            const newBurnerIndices = {};

            Object.keys(burners).forEach(burner => {
                newBurnerIndices[burner] = burners[burner].index;
                newDimensions[burner] = burners[burner].dimensions;
                newIdealParameters[burner] = burners[burner].parameters;
            });

            setIdealParameters(newIdealParameters);
            setDimensions(newDimensions);
            setBurnerIndices(newBurnerIndices);
        };
        reader.readAsText(file);
    }, []);

    const handleSave = () => {
        if (!selectedDataFile || !selectedParamsFile || !newBoilerHouseName) {
            alert('Пожалуйста, выберите файлы и введите название котельной установки.');
            return;
        }

        setIsLoading(true); // Показать индикатор загрузки

        // Включить индексы горелок в объект idealParameters
        const parametersWithIndices = {};
        Object.keys(idealParameters).forEach(burner => {
            const index = burnerIndices[burner];
            parametersWithIndices[index] = idealParameters[burner];
        });

        const formData = new FormData();
        formData.append('file', selectedDataFile); // Ensure the key matches what the server expects
        formData.append('paramsFile', selectedParamsFile);
        formData.append('name', newBoilerHouseName);
        formData.append('idealParameters', JSON.stringify(parametersWithIndices));

        fetch('http://localhost:8080/create-boiler-house', {
            method: 'POST',
            body: formData,
        })
            .then(response => {
                setIsLoading(false); // Скрыть индикатор загрузки
                if (response.status === 200) {
                    setShowSuccessMessage(true);
                    setTimeout(() => setShowSuccessMessage(false), 3000); // Скрыть через 3 секунды
                }
                return response.json();
            })
            .then(data => {
                setBoilerHouses([...boilerHouses, { name: newBoilerHouseName }]);
                setNewBoilerHouseName('');
                setSelectedDataFile(null);
                setSelectedParamsFile(null);
                setIdealParameters({});
                setDimensions({});
                setNumberOfBurners(0);
                setBurnerIndices({});
            })
            .catch(error => {
                setIsLoading(false); // Скрыть индикатор загрузки
                console.error('Ошибка при загрузке файла:', error);
            });
    };

    const handleIdealParameterChange = (burner, rowIndex, colIndex, value) => {
        setIdealParameters(prevState => {
            const newParams = { ...prevState };
            newParams[burner][rowIndex][colIndex] = parseFloat(value);
            return newParams;
        });
    };

    const handleDimensionChange = (burner, dimension, value) => {
        setDimensions(prevState => {
            const newDims = { ...prevState, [burner]: { ...prevState[burner], [dimension]: parseInt(value) || 0 } };
            setIdealParameters(prevParams => {
                const newParams = { ...prevParams };
                const cols = newDims[burner]?.cols || 0;
                newParams[burner] = [
                    Array(cols).fill(''), // Steam Capacity
                    Array(cols).fill(''), // Masut Pressure
                    Array(cols).fill('')  // Masut Consumption
                ];
                return newParams;
            });
            return newDims;
        });
    };

    const handleNumberOfBurnersChange = (value) => {
        const numBurners = parseInt(value) || 0;
        setNumberOfBurners(numBurners);

        const newIdealParameters = {};
        const newDimensions = {};
        const newBurnerIndices = {};
        for (let i = 1; i <= numBurners; i++) {
            newIdealParameters[i] = [
                [], // Steam Capacity
                [], // Masut Pressure
                []  // Masut Consumption
            ];
            newDimensions[i] = { cols: 0 };
            newBurnerIndices[i] = i;
        }
        setIdealParameters(newIdealParameters);
        setDimensions(newDimensions);
        setBurnerIndices(newBurnerIndices);
    };

    const handleBurnerIndexChange = (burner, value) => {
        const newIndex = parseInt(value) || 0;
        setBurnerIndices(prevState => {
            const newIndices = { ...prevState, [burner]: newIndex };
            return newIndices;
        });
    };

    const { getRootProps: getRootPropsData, getInputProps: getInputPropsData, isDragActive: isDragActiveData } = useDropzone({ onDrop: onDropData });
    const { getRootProps: getRootPropsParams, getInputProps: getInputPropsParams, isDragActive: isDragActiveParams } = useDropzone({ onDrop: onDropParams });

    return (
        <div className="container">
            <h1>Котельные установки</h1>
            <ul>
                {boilerHouses.map(bh => (
                    <li key={bh.name}>
                        <Link to={`/diagram/${bh.name}`} className="boiler-house-link">
                            {bh.name}
                        </Link>
                        <button onClick={() => handleDelete(bh.name)}>Удалить</button>
                    </li>
                ))}
            </ul>
            <div>
                <input
                    type="text"
                    value={newBoilerHouseName}
                    onChange={(e) => setNewBoilerHouseName(e.target.value)}
                    placeholder="Название новой котельной установки"
                />
                <div {...getRootPropsData()} className="dropzone">
                    <input {...getInputPropsData()} />
                    {isDragActiveData ? (
                        <p>Перетащите файлы сюда ...</p>
                    ) : (
                        <p>Данные работы котлоагрегата (.csv)</p>
                    )}
                    {selectedDataFile && <p>Выбранный файл: {selectedDataFile.name}</p>}
                </div>
                <div {...getRootPropsParams()} className="dropzone">
                    <input {...getInputPropsParams()} />
                    {isDragActiveParams ? (
                        <p>Перетащите файлы сюда ...</p>
                    ) : (
                        <p>Параметры работы котлоагрегата согласно режимной карте (.json)</p>
                    )}
                    {selectedParamsFile && <p>Выбранный файл: {selectedParamsFile.name}</p>}
                </div>
                <div>
                    <label>
                        Количество режимов работы котельной установки:
                        <input
                            type="number"
                            value={numberOfBurners}
                            onChange={(e) => handleNumberOfBurnersChange(e.target.value)}
                            placeholder="Количество режимов работы котельной установки"
                        />
                    </label>
                    {Object.keys(idealParameters).map(burner => (
                        <div key={burner}>
                            <h3>Режим {burnerIndices[burner]}</h3>
                            <label>
                                Кол-во горелок:
                                <input
                                    type="number"
                                    value={burnerIndices[burner]}
                                    onChange={(e) => handleBurnerIndexChange(burner, e.target.value)}
                                    placeholder="Индекс горелки"
                                />
                            </label>
                            <label>
                                Колонки:
                                <input
                                    type="number"
                                    value={dimensions[burner]?.cols || ''}
                                    onChange={(e) => handleDimensionChange(burner, 'cols', e.target.value)}
                                    placeholder="Количество колонок"
                                />
                            </label>
                            {idealParameters[burner].map((row, rowIndex) => (
                                <div key={rowIndex}>
                                    <h4>{['Производительность пара', 'Давление мазута', 'Расход мазута'][rowIndex]}</h4>
                                    {row.map((value, colIndex) => (
                                        <input
                                            key={colIndex}
                                            type="number"
                                            value={value}
                                            onChange={(e) => handleIdealParameterChange(burner, rowIndex, colIndex, e.target.value)}
                                            placeholder={`Параметр ${rowIndex + 1}.${colIndex + 1}`}
                                        />
                                    ))}
                                </div>
                            ))}
                        </div>
                    ))}
                </div>
                <button className="save" onClick={handleSave}>Сохранить</button>
            </div>
            {isLoading && (
                <div className="loading-indicator">
                    Сохранение...
                </div>
            )}
            {showSuccessMessage && (
                <div className="success-message">
                    Котельная установка успешно сохранена!
                </div>
            )}
        </div>
    );
};

export default MainPage;