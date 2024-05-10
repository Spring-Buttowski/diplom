//package com.example.diploma;
//
//import com.example.diploma.models.Coordinate;
//import com.example.diploma.services.DataService;
//import org.apache.commons.math3.fitting.PolynomialCurveFitter;
//import org.apache.commons.math3.fitting.WeightedObservedPoints;
//import org.jfree.chart.ChartFactory;
//import org.jfree.chart.ChartPanel;
//import org.jfree.chart.JFreeChart;
//import org.jfree.chart.plot.PlotOrientation;
//import org.jfree.data.xy.XYSeries;
//import org.jfree.data.xy.XYSeriesCollection;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import javax.swing.*;
//import java.awt.*;
//import java.time.LocalDateTime;
//import java.time.ZoneId;
//import java.time.format.DateTimeFormatter;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Locale;
//
//import static com.example.diploma.controllers.DataController.formatter;
//
//
//@SpringBootTest
//class DiplomaApplicationTests {
//
//    @Autowired
//    private DataService dataService;
//
//    @Test
//    void contextLoads() {
//    }
//
//    @Test
//    void showPlotTest() {
//        System.out.println(Math.pow(34, 0));
//        System.setProperty("java.awt.headless", "true");
//        LocalDateTime from = LocalDateTime.parse("06.01.2022 01:00:00", formatter);
//        LocalDateTime to = LocalDateTime.parse("08.01.2022 01:00:00", formatter);
//
//        List<Coordinate> coordinates = dataService.getData(from, to);
//
//        // Создаем набор наблюдаемых точек
//        WeightedObservedPoints obs = new WeightedObservedPoints();
//
//        // Добавляем данные (замените вашими значениями времени и значениями графика)
//        System.out.println(
//                coordinates.size()
//        );
//        for (Coordinate coordinate : coordinates) {
//            obs.add(coordinate.getTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), coordinate.getSteamCapacity());
//        }
//
//        // Создаем аппроксиматор с полиномом 2-й степени
//        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(4);
//        // Выполняем аппроксимацию и получаем коэффициенты полинома
//        double[] coefficients = fitter.fit(obs.toList());
//
//        // Выводим полученные коэффициенты
//        System.out.println("Коэффициенты полинома: " + Arrays.toString(coefficients));
//
//        double a = coefficients[0]/* ваш коэффициент a */;
//        double b = coefficients[1]/* ваш коэффициент b */;
//        double c = coefficients[2]/* ваш коэффициент c */;
//        double d = coefficients[3];
//        double f = coefficients[4];
//
//// Создание датасета
//        XYSeries series = new XYSeries("Аппроксимированные данные");
//
//        for (Coordinate coordinate : coordinates) {
//            { // Пример диапазона для X
//                double x = coordinate.getTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
//                double y = a * Math.pow(x, 4) + b * Math.pow(x, 3) + c * Math.pow(x, 2) + d * x + f;
//                series.add(x, y);
//            }
//        }
//
//        XYSeriesCollection dataset = new XYSeriesCollection(series);
//
//        JFreeChart chart = ChartFactory.createXYLineChart(
//                "Аппроксимация функции", // Название графика
//                "Время", // Название оси X
//                "Значение", // Название оси Y
//                dataset, // Датасет
//                PlotOrientation.VERTICAL,
//                true, // Показывать легенду
//                true,
//                false
//        );
//
//        ChartPanel chartPanel = new ChartPanel(chart);
//        chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
//        chartPanel.setBackground(Color.white);
//        JFrame frame = new JFrame();
//        frame.add(chartPanel);
//        frame.pack();
//        frame.setTitle("График аппроксимации");
//        frame.setLocationRelativeTo(null);
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.setVisible(true);
//
//    }
//
//    @Test
//    public void testKofs() {
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy H:mm", Locale.US);
//        String[] times = {
//                "06.01.2022 5:01",
//                "06.01.2022 5:02",
//                "06.01.2022 5:03",
//                "06.01.2022 5:04",
//                "06.01.2022 5:05",
//                "06.01.2022 5:06",
//                "06.01.2022 5:07",
//                "06.01.2022 5:08",
//                "06.01.2022 5:09",
//                "06.01.2022 5:10",
//                "06.01.2022 5:11",
//                "06.01.2022 5:12",
//                "06.01.2022 5:13",
//                "06.01.2022 5:14",
//                "06.01.2022 5:15"
//        };
//        String[] values = {
//                "34,926",
//                "34,79",
//                "34,714",
//                "34,648",
//                "34,569",
//                "34,481",
//                "34,524",
//                "34,509",
//                "34,498",
//                "34,515",
//                "34,547",
//                "34,595",
//                "34,608",
//                "34,676",
//                "34,666"
//        };
//        // Создаем набор наблюдаемых точек
//        WeightedObservedPoints obs = new WeightedObservedPoints();
//
//        // Исходное время для вычисления относительного времени в минутах
//        LocalDateTime startTime = LocalDateTime.parse(times[0], formatter);
//
//        // Преобразование данных и добавление их в набор точек
//        for (int i = 0; i < times.length; i++) {
//            LocalDateTime time = LocalDateTime.parse(times[i], formatter);
//            // Вычисление количества минут, прошедших с начала измерений
//            double x = java.time.Duration.between(startTime, time).toMinutes();
//            double y = Double.parseDouble(values[i].replace(",", "."));
//            obs.add(x, y);
//        }
//
//        // Создаем аппроксиматор с полиномом желаемой степени (например, 2)
//        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(2);
//        // Выполняем аппроксимацию и получаем коэффициенты полинома
//        double[] coefficients = fitter.fit(obs.toList());
//        System.out.println(Arrays.toString(coefficients));
//
//        for (int i = 0; i < times.length; i++) {
//            LocalDateTime time = LocalDateTime.parse(times[i], formatter);
//            double x = java.time.Duration.between(startTime, time).toMinutes();
//            double y = 0;
//            for (int currentPower = 0; currentPower < 2; currentPower++) {
//                y += coefficients[currentPower] * Math.pow(x, currentPower);
//            }
//            System.out.println(y);
//        }
//    }
//}
