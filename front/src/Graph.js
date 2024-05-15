import React, { useEffect, useRef, useState } from 'react';
import * as d3 from 'd3';
import DatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';
import { format } from 'date-fns';

const Graph = () => {
    const svgRef = useRef();
    const [startDate, setStartDate] = useState(new Date('2022-01-01 00:01'));
    const [endDate, setEndDate] = useState(new Date('2022-12-31 00:01'));
    const [showConsumption, setShowConsumption] = useState(false);

    useEffect(() => {
        const formatDate = (date) => format(date, 'yyyy-MM-dd HH:mm');

        const url = new URL('http://localhost:8080/coordinates');
        url.searchParams.append('dateFrom', formatDate(startDate));
        url.searchParams.append('dateTo', formatDate(endDate));
        url.searchParams.append('consumptionIsShown', showConsumption);

        fetch(url)
            .then(response => response.json())
            .then(data => {
                data.forEach(d => {
                    d.time = new Date(d.time);
                    d.burnersNum = +d.burnersNum;
                    d.steamCapacity = +d.steamCapacity;
                });

                const margin = { top: 20, right: 80, bottom: 30, left: 50 },
                    width = window.innerWidth - margin.left - margin.right - 100,
                    height = window.innerHeight - margin.top - margin.bottom - 100;

                const x = d3.scaleTime()
                    .domain(d3.extent(data, d => d.time))
                    .range([0, width]);

                // Custom scale for Y-axis
                const yBurnersNum = d3.scaleLinear()
                    .domain([0, 3, 5.5, 6])
                    .range([height, height, height * 0.1, 0]);

                const ySteamCapacity = d3.scaleLinear()
                    .domain(d3.extent(data, d => d.steamCapacity))
                    .range([height, 0])
                    .nice();

                const xAxis = d3.axisBottom(x);
                const yAxisLeft = d3.axisLeft(yBurnersNum)
                    .tickValues([0, 4, 5, 6])
                    .tickFormat(d3.format('d'));
                const yAxisRight = d3.axisRight(ySteamCapacity);

                d3.select(svgRef.current).selectAll('*').remove();

                const svg = d3.select(svgRef.current)
                    .attr('width', width + margin.left + margin.right)
                    .attr('height', height + margin.top + margin.bottom)
                    .append('g')
                    .attr('transform', `translate(${margin.left}, ${margin.top})`);

                svg.append('path')
                    .datum(data)
                    .attr('class', 'line')
                    .attr('fill', 'none')
                    .attr('stroke', 'steelblue')
                    .attr('stroke-width', 1.5)
                    .attr('d', d3.line()
                        .x(d => x(d.time))
                        .y(d => yBurnersNum(d.burnersNum)));

                svg.append('g')
                    .attr('transform', `translate(0, ${height})`)
                    .call(xAxis);

                svg.append('g')
                    .call(yAxisLeft);

                svg.append('g')
                    .attr('transform', `translate(${width}, 0)`)
                    .call(yAxisRight);
            })
            .catch(error => console.log(error));
    }, [startDate, endDate, showConsumption]);

    return (
        <div>
            <div style={{ marginBottom: '10px' }}>
                <label>
                    Start Date:
                    <DatePicker
                        selected={startDate}
                        onChange={(date) => setStartDate(date)}
                        dateFormat="yyyy-MM-dd HH:mm"
                        showTimeSelect
                        timeFormat="HH:mm"
                    />
                </label>
                <br/>
                <label>
                    End Date:
                    <DatePicker
                        selected={endDate}
                        onChange={(date) => setEndDate(date)}
                        dateFormat="yyyy-MM-dd HH:mm"
                        showTimeSelect
                        timeFormat="HH:mm"
                    />
                </label>
                <br/>
                <label>
                    Show Consumption:
                    <input type="checkbox"
                           checked={showConsumption}
                           onChange={(e) => setShowConsumption(e.target.checked)}
                           style={{marginLeft: '10px'}}/>
                </label>
            </div>
            <div style={{ display: 'flex', justifyContent: 'center' }}>
                <svg ref={svgRef}/>
            </div>
        </div>
    );
};

export default Graph;
