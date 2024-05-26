import React, { useEffect, useRef, useState } from 'react';
import * as d3 from 'd3';
import DatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';
import { format } from 'date-fns';
import { useParams } from 'react-router-dom';

const DiagramPage = () => {
    const { boilerHouseName } = useParams(); // Extract boiler house name from URL
    const svgRef = useRef();
    const [startDate, setStartDate] = useState(null);
    const [endDate, setEndDate] = useState(null);
    const [showCapacity, setShowCapacity] = useState(false);
    const [showBurners, setShowBurners] = useState(true);

    useEffect(() => {
        // Fetch boiler house details to get min and max dates
        fetch('http://localhost:8080/boiler-houses')
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

                const margin = { top: 100, right: 80, bottom: 70, left: 50 }, // Increased top margin for legend
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

                if (showBurners) {
                    lineGroup.append('path')
                        .datum(data)
                        .attr('class', 'line blue')
                        .attr('fill', 'none')
                        .attr('stroke', 'steelblue')
                        .attr('stroke-width', 2.5)
                        .attr('d', d3.line()
                            .x(d => x(d.time))
                            .y(d => yBurnersNum(d.burnersNum)));
                }

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
                    .attr("transform", "rotate(-30)"); // Rotate labels

                if (showBurners) {
                    svg.append('g')
                        .attr('class', 'y-axis-left')
                        .call(yAxisLeft)
                        .selectAll("text")
                        .style("font-size", "14px"); // Increase font size
                }

                if (showCapacity) {
                    if (showBurners) {
                        svg.append('g')
                            .attr('class', 'y-axis-right')
                            .attr('transform', `translate(${width}, 0)`)
                            .call(yAxisRight)
                            .selectAll("text")
                            .style("font-size", "14px"); // Increase font size
                    } else {
                        svg.append('g')
                            .attr('class', 'y-axis-left')
                            .call(yAxisRight
                                .tickSize(-5) // Make ticks point inward
                                .tickPadding(1) // Adjust padding for labels
                            )
                            .selectAll("text")
                            .style("font-size", "14px")
                            .style("text-anchor", "end") // Align text to the end (left)
                            .attr("dx", "-.8em"); // Move text to the left
                    }
                }

                // Add X axis label
                svg.append("text")
                    .attr("class", "x-axis-label")
                    .attr("text-anchor", "middle")
                    .attr("x", width / 2)
                    .attr("y", height + margin.bottom - 5)
                    .text("Время");

                // Add Y left axis label
                if (showBurners) {
                    svg.append("text")
                        .attr("class", "y-axis-left-label")
                        .attr("text-anchor", "middle")
                        .attr("transform", `translate(${-margin.left + 20}, ${height / 2}) rotate(-90)`)
                        .text("Количество включенных горелок");
                }

                // Add Y right axis label
                if (showCapacity) {
                    if (showBurners) {
                        svg.append("text")
                            .attr("class", "y-axis-right-label")
                            .attr("text-anchor", "middle")
                            .attr("transform", `translate(${width + margin.right - 20}, ${height / 2}) rotate(-90)`)
                            .text("Паропроизводительность, т/час");
                    } else {
                        svg.append("text")
                            .attr("class", "y-axis-left-label")
                            .attr("text-anchor", "middle")
                            .attr("transform", `translate(${-margin.left + 20}, ${height / 2}) rotate(-90)`)
                            .text("Паропроизводительность, т/час");
                    }
                }

                // Add legend
                const legend = svg.append('g')
                    .attr('class', 'legend')
                    .attr('transform', `translate(0, -80)`); // Move legend above the chart

                if (showBurners) {
                    legend.append('rect')
                        .attr('x', 0)
                        .attr('y', 0)
                        .attr('width', 20)
                        .attr('height', 20)
                        .attr('fill', 'steelblue');

                    legend.append('text')
                        .attr('x', 30)
                        .attr('y', 15)
                        .text('Количество включенных горелок')
                        .style('font-size', '14px');
                }

                if (showCapacity) {
                    legend.append('rect')
                        .attr('x', 0)
                        .attr('y', showBurners ? 30 : 0) // Adjust position if both legends are shown
                        .attr('width', 20)
                        .attr('height', 20)
                        .attr('fill', 'red');

                    legend.append('text')
                        .attr('x', 30)
                        .attr('y', showBurners ? 45 : 15) // Adjust position if both legends are shown
                        .text('Паропроизводительность, т/час')
                        .style('font-size', '14px');
                }

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

                    if (showBurners) {
                        lineGroup.selectAll('.line.blue')
                            .attr('d', d3.line()
                                .x(d => newX(d.time))
                                .y(d => yBurnersNum(d.burnersNum)));
                    }

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
    }, [startDate, endDate, showCapacity, showBurners, boilerHouseName]);

    const downloadImage = () => {
        const svgElement = svgRef.current;
        const svgString = new XMLSerializer().serializeToString(svgElement);
        const canvas = document.createElement('canvas');
        const ctx = canvas.getContext('2d');
        const DOMURL = window.URL || window.webkitURL || window;

        const img = new Image();
        const svgBlob = new Blob([svgString], { type: 'image/svg+xml;charset=utf-8' });
        const url = DOMURL.createObjectURL(svgBlob);

        img.onload = () => {
            canvas.width = svgElement.clientWidth;
            canvas.height = svgElement.clientHeight;
            ctx.drawImage(img, 0, 0);
            DOMURL.revokeObjectURL(url);

            const imgURI = canvas
                .toDataURL('image/png')
                .replace('image/png', 'image/octet-stream');

            const a = document.createElement('a');
            a.setAttribute('download', 'diagram.png');
            a.setAttribute('href', imgURI);
            a.setAttribute('target', '_blank');
            a.click();
        };

        img.src = url;
    };

    return (
        <div>
            <div style={{ marginLeft: '50px', marginTop: '5px', display: 'flex', alignItems: 'center' }}>
                <div>
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
                    <br />
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
                </div>
                <div style={{ marginLeft: '20px' }}>
                    <label>
                        Отобразить количество включенных горелок:
                        <input type="checkbox"
                               checked={showBurners}
                               onChange={(e) => setShowBurners(e.target.checked)}
                               style={{ marginLeft: '10px' }} />
                    </label>
                    <br />
                    <label>
                        Отобразить паропроизводительность:
                        <input type="checkbox"
                               checked={showCapacity}
                               onChange={(e) => setShowCapacity(e.target.checked)}
                               style={{ marginLeft: '10px' }} />
                    </label>
                    <br />
                    <button onClick={downloadImage}>Download as PNG</button>
                </div>
            </div>
            <div style={{ display: 'flex', justifyContent: 'center' }}>
                <svg ref={svgRef} />
            </div>
        </div>
    );
};

export default DiagramPage;