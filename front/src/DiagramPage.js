import React, {useEffect, useRef, useState} from 'react';
import * as d3 from 'd3';
import DatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';
import {format} from 'date-fns';
import {useParams} from 'react-router-dom';

const DiagramPage = () => {
    const {boilerHouseName} = useParams(); // Extract boiler house name from URL
    const svgRef = useRef();
    const [startDate, setStartDate] = useState(null);
    const [endDate, setEndDate] = useState(null);
    const [showCapacity, setShowCapacity] = useState(false);

    useEffect(() => {
        // Fetch boiler house details to get min and max dates
        fetch(`http://localhost:8080/boiler-houses`)
            .then(response => response.json())
            .then(data => {
                const boilerHouse = data.find(bh => bh.name === boilerHouseName);
                if (boilerHouse) {
                    setStartDate(new Date(boilerHouse.minDate));
                    setEndDate(new Date(boilerHouse.maxDate));
                }
            })
            .catch(error => console.log(error));
    }, [boilerHouseName]);

    useEffect(() => {
        if (!startDate || !endDate) return;

        const formatDate = (date) => format(date, 'yyyy-MM-dd HH:mm');

        const url = new URL('http://localhost:8080/coordinates-by-boiler-house');
        url.searchParams.append('dateFrom', formatDate(startDate));
        url.searchParams.append('dateTo', formatDate(endDate));
        url.searchParams.append('name', boilerHouseName); // Use the dynamic boiler house name

        fetch(url)
            .then(response => response.json())
            .then(data => {
                data.forEach(d => {
                    d.time = new Date(d.time);
                    d.burnersNum = +d.burnersNum;
                    d.steamCapacity = +d.steamCapacity;
                });

                const margin = {top: 20, right: 80, bottom: 70, left: 50},
                    width = window.innerWidth - margin.left - margin.right - 200, // Reduced width
                    height = window.innerHeight - margin.top - margin.bottom - 200; // Reduced height

                const x = d3.scaleTime()
                    .domain(d3.extent(data, d => d.time))
                    .range([0, width]);

                const yBurnersNum = d3.scaleLinear()
                    .domain([0, 6])
                    .range([height, 0]);

                const ySteamCapacity = d3.scaleLinear()
                    .domain(d3.extent(data, d => d.steamCapacity))
                    .range([height, 0])
                    .nice();

                const xAxis = d3.axisBottom(x)
                    .tickFormat(d3.timeFormat("%Y-%m-%d")); // Format can be adjusted

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

                // Define the clipping path
                svg.append('defs').append('clipPath')
                    .attr('id', 'clip')
                    .append('rect')
                    .attr('width', width)
                    .attr('height', height);

                // Group for the line paths
                const lineGroup = svg.append('g')
                    .attr('clip-path', 'url(#clip)');

                lineGroup.append('path')
                    .datum(data)
                    .attr('class', 'line blue')
                    .attr('fill', 'none')
                    .attr('stroke', 'steelblue')
                    .attr('stroke-width', 2.5)
                    .attr('d', d3.line()
                        .x(d => x(d.time))
                        .y(d => yBurnersNum(d.burnersNum)));

                if (showCapacity) {
                    lineGroup.append('path')
                        .datum(data)
                        .attr('class', 'line red')
                        .attr('fill', 'none')
                        .attr('stroke', 'red')
                        .attr('stroke-width', 2.5)
                        .attr('d', d3.line()
                            .x(d => x(d.time))
                            .y(d => ySteamCapacity(d.steamCapacity)));
                }

                svg.append('g')
                    .attr('class', 'x-axis')
                    .attr('transform', `translate(0, ${height})`)
                    .call(xAxis)
                    .selectAll("text")
                    .style("text-anchor", "end")
                    .style("font-size", "14px") // Increase font size
                    .attr("dx", "-.8em")
                    .attr("dy", ".15em")
                    .attr("transform", "rotate(-35)"); // Rotate labels

                svg.append('g')
                    .attr('class', 'y-axis-left')
                    .call(yAxisLeft)
                    .selectAll("text")
                    .style("font-size", "14px"); // Increase font size

                svg.append('g')
                    .attr('class', 'y-axis-right')
                    .attr('transform', `translate(${width}, 0)`)
                    .call(yAxisRight)
                    .selectAll("text")
                    .style("font-size", "14px"); // Increase font size

                const zoom = d3.zoom()
                    .scaleExtent([0.5, 20])
                    .translateExtent([[0, 0], [width, height]])
                    .extent([[0, 0], [width, height]])
                    .on('zoom', zoomed);

                svg.append('rect')
                    .attr('width', width)
                    .attr('height', height)
                    .style('fill', 'none')
                    .style('pointer-events', 'all')
                    .attr('transform', `translate(${margin.left}, ${margin.top})`)
                    .call(zoom);

                function zoomed(event) {
                    const transform = event.transform;
                    const newX = transform.rescaleX(x);

                    lineGroup.selectAll('.line.blue')
                        .attr('d', d3.line()
                            .x(d => newX(d.time))
                            .y(d => yBurnersNum(d.burnersNum)));

                    if (showCapacity) {
                        lineGroup.selectAll('.line.red')
                            .attr('d', d3.line()
                                .x(d => newX(d.time))
                                .y(d => ySteamCapacity(d.steamCapacity)));
                    }

                    svg.select('.x-axis').call(d3.axisBottom(newX));
                }
            })
            .catch(error => console.log(error));
    }, [startDate, endDate, showCapacity, boilerHouseName]);

    return (
        <div>
            <div style={{marginLeft: '50px', marginTop: '5px'}}>
                <label>
                    От:
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
                    До:
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
                    Отобразить паропроизводительность:
                    <input type="checkbox"
                           checked={showCapacity}
                           onChange={(e) => setShowCapacity(e.target.checked)}
                           style={{marginLeft: '10px'}}/>
                </label>
            </div>
            <div style={{display: 'flex', justifyContent: 'center'}}>
                <svg ref={svgRef}/>
            </div>
        </div>
    );
};

export default DiagramPage;