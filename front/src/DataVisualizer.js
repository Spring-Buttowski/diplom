import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend } from 'recharts';
import DatePicker from 'react-datepicker';
import "react-datepicker/dist/react-datepicker.css";

const DataVisualizer = () => {
    const [startDate, setStartDate] = useState(new Date());
    const [endDate, setEndDate] = useState(new Date());
    const [data, setData] = useState([]);

    useEffect(() => {
        fetchData();
    }, [startDate, endDate]);

    const formatDate = (date) => {
        return `${date.getFullYear()}.${(date.getMonth() + 1).toString().padStart(2, '0')}.${date.getDate().toString().padStart(2, '0')} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}:${date.getSeconds().toString().padStart(2, '0')}`;
    };

    const fetchData = async () => {
        try {
            const response = await axios.get(`http://localhost:8080/coordinates?dateFrom=${formatDate(startDate)}&dateTo=${formatDate(endDate)}&consumptionIsShown=false`);
            setData(response.data);
        } catch (error) {
            console.error('Error fetching data:', error);
            setData([]);
        }
    };

    return (
        <div>
            <h2>Data Visualization</h2>
            <div>
                <label>Start Date: </label>
                <DatePicker selected={startDate} onChange={date => setStartDate(date)} showTimeSelect dateFormat="MMMM d, yyyy h:mm aa" />
            </div>
            <div>
                <label>End Date: </label>
                <DatePicker selected={endDate} onChange={date => setEndDate(date)} showTimeSelect dateFormat="MMMM d, yyyy h:mm aa" />
            </div>
            <LineChart width={600} height={300} data={data} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="time" />
                <YAxis type="number" domain={[0, 6]} allowDataOverflow={true} />
                <Tooltip />
                <Legend />
                <Line type="monotone" dataKey="burnersNum" stroke="#8884d8" activeDot={{ r: 8 }} />
            </LineChart>
        </div>
    );
};

export default DataVisualizer;
