package spring.buttowski.diploma.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.apache.poi.util.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import spring.buttowski.diploma.controllers.DataController;
import spring.buttowski.diploma.models.BoilerHouse;
import spring.buttowski.diploma.models.Coordinate;
import spring.buttowski.diploma.models.Data;
import spring.buttowski.diploma.repositories.BoilerHouseRepository;
import spring.buttowski.diploma.repositories.CoordinateRepository;
import spring.buttowski.diploma.repositories.DataRepository;
import spring.buttowski.diploma.util.XlsToProductListParser;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Service
@Slf4j
public class DataService {
    private final DataRepository dataRepository;
    private final XlsToProductListParser productListParser;
    private final BoilerHouseRepository boilerHouseRepository;
    private final CoordinateRepository coordinateRepository;

    private static final int MAXIMUM_ZERO_GAP = 60;
    private static final double NEAR_ZERO = 0.0001;
    private static final int IMPLICIT_GAP_MAXIMUM = 120;

    @Autowired
    public DataService(DataRepository dataRepository, XlsToProductListParser productListParser, BoilerHouseRepository boilerHouseRepository, CoordinateRepository coordinateRepository) {
        this.dataRepository = dataRepository;
        this.productListParser = productListParser;
        this.boilerHouseRepository = boilerHouseRepository;
        this.coordinateRepository = coordinateRepository;
    }

    public List<Coordinate> getData(LocalDateTime dateFrom, LocalDateTime dateTo, String boilerHouseName) {
        BoilerHouse boilerHouse = boilerHouseRepository.findByName(boilerHouseName).get();
        //Находим все нужные нам значения параметров работы за определённый промежуток времени
        List<Coordinate> coordinates = coordinateRepository.findDataByTimeBetweenAndBoilerHouse(dateFrom, dateTo, boilerHouse, Sort.by("time"));
        if (dateFrom.equals(dateTo)) {
            return Collections.emptyList();
        }
        countGaps(coordinates);
        return coordinates;
    }

    public static int countGaps(List<Coordinate> coordinates) {
        System.out.println("-------------");
        Iterator<Coordinate> iterator = coordinates.iterator();
        Coordinate gapStart, next;
        int timeCounter;
        int implicitGapsCounter = 0;
        while (iterator.hasNext()) {
            for (gapStart = iterator.next(), next = iterator.next(), timeCounter = 0;
                 iterator.hasNext() && gapStart.getBurnersNum() == next.getBurnersNum();
                 next = iterator.next()) {
                timeCounter++;
            }
            String alarm = "";
            if (timeCounter < IMPLICIT_GAP_MAXIMUM) {
                implicitGapsCounter++;
                alarm = " <60 минут!";
            }
            System.out.println(gapStart.getTime().format(DataController.formatter) +
                    " - " + next.getTime().minusMinutes(1).format(DataController.formatter)
                    + " " + gapStart.getBurnersNum() + " " + alarm);
        }
        System.out.println("-------------(Implicit gaps' amount - " + implicitGapsCounter + ")");
//        System.out.print(implicitGapsCounter + "\n");
        return implicitGapsCounter;
    }

    //TODO Можно оптимизировать путём использования суммы с прошлой итерации
    public static void movingAverage(List<Data> dataList, int border) {
        Data[] dataArray = dataList.toArray(new Data[0]);
        double sumSteamCapacity = 0.0;
        double sumMasutPresure = 0.0;
        double sumMasutConsumption = 0.0;
        for (int outerIndex = 0; outerIndex < dataList.size(); outerIndex++) {
            int from = Math.max(0, outerIndex - border);
            int to = Math.min(dataList.size() - 1, outerIndex + border);
            for (int innerIndex = from;
                 innerIndex <= to;
                 innerIndex++) {
                sumSteamCapacity += dataArray[innerIndex].getSteamCapacity();
                sumMasutConsumption += dataArray[innerIndex].getMasutConsumtion();
                sumMasutPresure += dataArray[innerIndex].getMasutPresure();
            }
            int quantity = to - from + 1;
            dataList.get(outerIndex).setSteamCapacity(sumSteamCapacity / quantity);
            dataList.get(outerIndex).setMasutPresure(sumMasutPresure / quantity);
            dataList.get(outerIndex).setMasutConsumtion(sumMasutConsumption / quantity);
            sumSteamCapacity = 0.0;
            sumMasutConsumption = 0.0;
            sumMasutPresure = 0.0;
        }
    }

    public static <T> void interpolateZerosAndNull(List<T> data, Function<T, Double> getter, BiConsumer<T, Double> setter) {
        int n = data.size();

        for (int i = 0; i < n; i++) {
            if (Math.abs(getter.apply(data.get(i))) < NEAR_ZERO) {
                int counter = i;
                while (counter < n && Math.abs(getter.apply(data.get(counter))) < NEAR_ZERO) {
                    counter++;
                }
                if (counter - i > MAXIMUM_ZERO_GAP) {
                    i = counter - 1;
                    continue;
                }
                // Find the nearest non-zero values before and after the zero
                int left = i - 1;
                int right = i + 1;

                while (left >= 0 && Math.abs(getter.apply(data.get(left))) < NEAR_ZERO) {
                    left--;
                }

                while (right < n && Math.abs(getter.apply(data.get(right))) < NEAR_ZERO) {
                    right++;
                }

                if (left >= 0 && right < n) {
                    // Perform linear interpolation
                    double interpolatedValue = getter.apply(data.get(left)) +
                            (getter.apply(data.get(right)) - getter.apply(data.get(left))) * (i - left) / (right - left);
                    setter.accept(data.get(i), interpolatedValue);
                } else if (left >= 0) {
                    // If there is no right non-zero value, use the left value
                    setter.accept(data.get(i), getter.apply(data.get(left)));
                } else if (right < n) {
                    // If there is no left non-zero value, use the right value
                    setter.accept(data.get(i), getter.apply(data.get(right)));
                }
            }
        }
    }

    public void saveBoilerHouse(MultipartFile file, String boilerHouseName, String idealParametersString) {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<Integer, double[][]> idealParameters = new HashMap<>();
        try {
            idealParameters = objectMapper.readValue(idealParametersString, new TypeReference<>() {
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        idealParameters.put(0, new double[][]{{0}, {0}, {0}});

        IOUtils.setByteArrayMaxOverride(Integer.MAX_VALUE);
        BoilerHouse boilerHouse = boilerHouseRepository.save(BoilerHouse
                .builder()
                .name(boilerHouseName)
                .build());

        List<Data> dataList = productListParser.parseCSVToProducts(file, boilerHouse);

        boilerHouse.setMaxDate(dataList.get(dataList.size() - 1).getTime());
        boilerHouse.setMinDate(dataList.get(0).getTime());

        boilerHouseRepository.save(boilerHouse);
        log.info("Evaluating coordinates");

        //Интерполируем значения где датчики дали сбой
        interpolateZerosAndNull(dataList, Data::getSteamCapacity, Data::setSteamCapacity);
        interpolateZerosAndNull(dataList, Data::getMasutConsumtion, Data::setMasutConsumtion);
        interpolateZerosAndNull(dataList, Data::getMasutPresure, Data::setMasutPresure);

        //"Сглаживаем" данные методом кользящего среднего
        movingAverage(dataList, 100);

        log.info("Saving data");
        dataRepository.saveAll(dataList);


//        int gap = 50;
//        int degree = 2;
//        for (int start = 0, end = gap; end < dataList.size(); start += gap, end += gap) {
//            approximate(dataList.subList(start, end), Data::getTime, Data::getMasutPresure, Data::setMasutPresure, degree);
//            approximate(dataList.subList(start, end), Data::getTime, Data::getMasutConsumtion, Data::setMasutConsumtion, degree);
//            approximate(dataList.subList(start, end), Data::getTime, Data::getSteamCapacity, Data::setSteamCapacity, degree);
//        }

        //Считаем кол-во включенных горелок для каждого момента времени
        List<Coordinate> coordinates = getBurnersAmountByClusterization(dataList, boilerHouse, idealParameters);
        log.info("Saving coordinates");

        coordinateRepository.saveAll(coordinates);
        log.info(boilerHouseName + " has been saved");
    }


    public static List<Coordinate> getBurnersAmountByClusterization(List<Data> dataList,
                                                                    BoilerHouse boilerHouse,
                                                                    Map<Integer, double[][]> idealParameters) {
        List<Coordinate> coordinates = new ArrayList<>();
        double minDistance = Double.MAX_VALUE;
        double currentDistance;
        int burnersNum = -1;

        Map<Integer, double[]> idealParametersNew = new HashMap<>();
        for (int key : idealParameters.keySet()) {
            double[][] array = idealParameters.get(key);
            double[] arrayNew = new double[3];
            for (int i = 0; i < array.length; i++) {
                double[] arr = array[i];
                double sum = 0;
                for (double v : arr) {
                    sum += v;
                }
                sum /= arr.length;
                arrayNew[i] = sum;
            }
            idealParametersNew.put(key, arrayNew);
        }


        //Проходим по всем значениям
        for (Data dataPoint : dataList) {
//            if (dataPoint.getTime().toString().equals("2023-01-02T18:50")) {
//                System.out.println();
//            }

            //После выполнения это цикла, мы точно можем сказать сколько горелок было включено в текузий момент
            for (int burnersNumByTable : idealParametersNew.keySet()) {
                double[] array = idealParametersNew.get(burnersNumByTable);
//                if (array[0][0] <= dataPoint.getSteamCapacity() && dataPoint.getSteamCapacity() <= array[0][array[0].length - 1]) {
//                    burnersNum = burnersNumByTable;
//                    break;
//                } else {
                currentDistance = Math.sqrt(Math.pow(dataPoint.getMasutPresure() - array[1], 2)
                        + Math.pow(dataPoint.getMasutConsumtion() - array[2], 2)
                        + Math.pow(dataPoint.getSteamCapacity() - array[0], 2));
//                            currentDistance = Math.abs(100 - 100 * dataPoint.getMasutPresure() / array[1][i]) +
//                                    Math.abs(100 - 100 * dataPoint.getMasutConsumtion() / array[2][i]) +
//                                    Math.abs(100 - 100 * dataPoint.getSteamCapacity() / array[0][i]);
                if (currentDistance < minDistance) {
                    minDistance = currentDistance;
                    burnersNum = burnersNumByTable;
                }

//                }
            }

            minDistance = Double.MAX_VALUE;


            coordinates.add(Coordinate.builder()
                    .time(dataPoint.getTime())
                    .burnersNum(burnersNum)
                    .steamCapacity(dataPoint.getSteamCapacity())
                    .boilerHouse(boilerHouse)
//                    .masutPresure(showCapacity ? dataPoint.getMasutPresure() : null)
//                    .masutConsumption(showCapacity ? dataPoint.getMasutConsumtion() : null
                    .build());
        }
        // Add more data points as needed


        return coordinates;
    }

    public static <DATA> void approximate(List<DATA> dataList, Function<DATA, LocalDateTime> dataTimeGetter,
                                          Function<DATA, Double> dataSteamCapacityGetter,
                                          BiConsumer<DATA, Double> coordinateSetter, int degree) {
        // Perform polynomial regression
        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(degree);
        WeightedObservedPoints points = new WeightedObservedPoints();

        for (DATA data : dataList) {
            double x = dataTimeGetter.apply(data).toEpochSecond(ZoneOffset.UTC);
            double y = dataSteamCapacityGetter.apply(data);
            points.add(x, y);
        }

        // Fit the polynomial to the data
        double[] coefficients = fitter.fit(points.toList());

        // Output the polynomial coefficients
//        for (int i = 0; i < coefficients.length; i++) {
//            System.out.println("Coefficient of x^" + i + ": " + coefficients[i]);
//        }

        for (DATA data : dataList) {
            double y = 0.0;
            double x = dataTimeGetter.apply(data).toEpochSecond(ZoneOffset.UTC);
            for (int i = 0; i < coefficients.length; i++) {
                y += coefficients[i] * Math.pow(x, i);
            }
            coordinateSetter.accept(data, y);
        }
    }


    public List<BoilerHouse> getBoilerHouses() {
        return boilerHouseRepository.findAll();
    }

    @Transactional
    public void deleteBoilerHouse(String name) {
        boilerHouseRepository.deleteByName(name);
    }
}
