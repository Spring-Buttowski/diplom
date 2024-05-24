package spring.buttowski.diploma.services;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.springframework.data.domain.Sort;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.data.vector.DoubleVector;
import smile.regression.LinearModel;
import smile.regression.OLS;
import spring.buttowski.diploma.controllers.DataController;
import spring.buttowski.diploma.models.BoilerHouse;
import spring.buttowski.diploma.models.Coordinate;
import spring.buttowski.diploma.models.Data;
import spring.buttowski.diploma.repositories.BoilerHouseRepository;
import spring.buttowski.diploma.repositories.CoordinateRepository;
import spring.buttowski.diploma.repositories.DataRepository;
import spring.buttowski.diploma.util.XlsToProductListParser;
import org.apache.poi.util.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
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

    public static void countGaps(List<Coordinate> coordinates) {
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
            if (timeCounter < 60) {
                implicitGapsCounter++;
                alarm = " <60 минут!";
            }
//            System.out.println(gapStart.getTime().format(DataController.formatter) +
//                    " - " + next.getTime().minusMinutes(1).format(DataController.formatter)
//                    + " " + gapStart.getBurnersNum() + " " + alarm);
        }
//        System.out.println("-------------(Implicit gaps' amount - " + implicitGapsCounter + ")");
        System.out.println(implicitGapsCounter);
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

    private static final int MAXIMUM_ZERO_GAP = 60;
    private static final double NEAR_ZERO = 0.0001;

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

    public void saveBoilerHouse(MultipartFile file, String boilerHouseName, Map<Integer, double[][]> idealParameters) {
        IOUtils.setByteArrayMaxOverride(Integer.MAX_VALUE);
        BoilerHouse boilerHouse = boilerHouseRepository.save(BoilerHouse
                .builder()
                .name(boilerHouseName)
                .build());

//        log.info("Saving data");
        List<Data> dataList = productListParser.parseCSVToProducts(file, boilerHouse);

        boilerHouse.setMaxDate(dataList.get(dataList.size() - 1).getTime());
        boilerHouse.setMinDate(dataList.get(0).getTime());

        boilerHouseRepository.save(boilerHouse);
        log.info("Evaluating coordinates");

        //Интерполируем значения где датчики дали сбой
        interpolateZerosAndNull(dataList, Data::getSteamCapacity, Data::setSteamCapacity);
        interpolateZerosAndNull(dataList, Data::getMasutConsumtion, Data::setMasutConsumtion);
        interpolateZerosAndNull(dataList, Data::getMasutPresure, Data::setMasutPresure);
//        dataRepository.saveAll(dataList);
        //"Сглаживаем" данные методом кользящего среднего
        movingAverage(dataList, 80);

//        approximate(dataList, Data::getTime, Data::getMasutPresure, Data::setMasutPresure);
//        approximate(dataList, Data::getTime, Data::getMasutConsumtion, Data::setMasutConsumtion);
//        approximate(dataList, Data::getTime, Data::getSteamCapacity, Data::setSteamCapacity);


        //Считаем кол-во включенных горелок для каждого момента времени
        List<Coordinate> coordinates = getBurnersAmountByClusterization2(dataList, boilerHouse, idealParameters);
        log.info("Saving coordinates");

        coordinateRepository.saveAll(coordinates);
        log.info(boilerHouseName + " has been saved");
    }

    public List<BoilerHouse> getBoilerHouses() {
        return boilerHouseRepository.findAll();
    }

    @Transactional
    public void deleteBoilerHouse(String name) {
        boilerHouseRepository.deleteByName(name);
    }

    public static List<Coordinate> getBurnersAmountByClusterizationOLD(List<Data> dataList, BoilerHouse boilerHouse, Map<Integer, double[][]> idealParameters) {
        List<Coordinate> coordinates = new ArrayList<>();
        double minDistance = Double.MAX_VALUE;
        double currentDistance;
        int burnersNum = -1;
        double lastSteamCapacity;
        int lastBurnersNum = 0;
        double[][] array;
        Set<Integer> keySet;
        lastSteamCapacity = dataList.get(0).getSteamCapacity();

        //Проходим по всем значениям
        for (Data data : dataList) {
            //Данные с котлоагрегата могут быть сняты неправильно.
            //Если произошёл большой скачок в паропроизводительности, то в этот момент времеи занчения были сняты неправлиьно.
            //Присваиваем кол-во горелок равному на прошлой итерации
            if (Math.abs(data.getSteamCapacity() - lastSteamCapacity) > 20) {
                burnersNum = lastBurnersNum;
            } else {
                //Проверяем работал ли котлаграгет впринципе в этот момент времени
                if (Math.abs(data.getMasutPresure()) < 0.1 || Math.abs(data.getMasutConsumtion()) < 0.1 || Math.abs(data.getSteamCapacity()) < 0.1) {
                    //Котлоагргат не работал
                    burnersNum = 0;
                } else {
                    keySet = idealParameters.keySet();
                    //После выполнения это цикла, мы точно можем сказать сколько горелок было включено в текузий момент
                    for (int burnersNumByTable : keySet) {
                        array = idealParameters.get(burnersNumByTable);
                        for (int i = 0; i < array[0].length; i++) {
                            currentDistance = Math.sqrt(Math.pow(data.getMasutPresure() - array[0][i], 2)
                                    + Math.pow(data.getMasutConsumtion() - array[1][i], 2)
                                    + Math.pow(data.getSteamCapacity() - array[2][i], 2));
//                            currentDistance = Math.abs(100 - 100 * data.getMasutPresure() / array[0][i]) +
//                                    Math.abs(100 - 100 * data.getMasutConsumtion() / array[1][i]) +
//                                    Math.abs(100 - 100 * data.getSteamCapacity() / array[2][i]);
                            if (currentDistance < minDistance) {
                                minDistance = currentDistance;
                                burnersNum = burnersNumByTable;
                                lastBurnersNum = burnersNum;
                            }
                        }
                    }
                }
                minDistance = Double.MAX_VALUE;
                lastSteamCapacity = data.getSteamCapacity();
            }
            coordinates.add(Coordinate.builder()
                    .time(data.getTime())
                    .burnersNum(burnersNum)
                    .steamCapacity(data.getSteamCapacity())
                    .boilerHouse(boilerHouse)
//                    .masutPresure(showCapacity ? data.getMasutPresure() : null)
//                    .masutConsumption(showCapacity ? data.getMasutConsumtion() : null
                    .build());
        }

        return coordinates;
    }

    private static List<Coordinate> getBurnersAmountByClusterization(List<Data> dataList,
                                                                     BoilerHouse boilerHouse,
                                                                     Map<Integer, double[][]> idealParameters) {
        List<Coordinate> coordinates = new ArrayList<>();
        double minDistance = Double.MAX_VALUE;
        double currentDistance;
        int burnersNum = -1;
        double lastSteamCapacity;
        double lastMasutPresure;
        double lastMasutConsumption;
        int lastBurnersNum = 0;
        double[][] array;
        Set<Integer> keySet;
        lastSteamCapacity = dataList.get(0).getSteamCapacity();
        lastMasutPresure = dataList.get(0).getMasutPresure();
        lastMasutConsumption = dataList.get(0).getMasutConsumtion();

        //Проходим по всем значениям
        for (Data data : dataList) {
            //Данные с котлоагрегата могут быть сняты неправильно.
            //Если произошёл большой скачок в паропроизводительности, то в этот момент времеи занчения были сняты неправлиьно.
            //Присваиваем кол-во горелок равному на прошлой итерации
            if (Math.abs(data.getSteamCapacity() - lastSteamCapacity) > 15 ||
                    Math.abs(data.getMasutConsumtion() - lastMasutConsumption) > 2
                    || Math.abs(data.getMasutPresure() - lastMasutPresure) > 10) {
                burnersNum = lastBurnersNum;
            } else {
                //Проверяем работал ли котлаграгет впринципе в этот момент времени
                if (Math.abs(data.getMasutPresure()) < 0.1 || Math.abs(data.getMasutConsumtion()) < 0.1 || Math.abs(data.getSteamCapacity()) < 0.1) {
                    //Котлоагргат не работал
                    burnersNum = 0;
                } else {
                    keySet = idealParameters.keySet();
                    //После выполнения это цикла, мы точно можем сказать сколько горелок было включено в текузий момент
                    for (int burnersNumByTable : keySet) {
                        array = idealParameters.get(burnersNumByTable);
                        for (int i = 0; i < array[0].length; i++) {
                            currentDistance = Math.sqrt(Math.pow(data.getMasutPresure() - array[1][i], 2)
                                    + Math.pow(data.getMasutConsumtion() - array[2][i], 2)
                                    + Math.pow(data.getSteamCapacity() - array[0][i], 2));
//                            currentDistance = Math.abs(100 - 100 * data.getMasutPresure() / array[1][i]) +
//                                    Math.abs(100 - 100 * data.getMasutConsumtion() / array[2][i]) +
//                                    Math.abs(100 - 100 * data.getSteamCapacity() / array[0][i]);
                            if (currentDistance < minDistance) {
                                minDistance = currentDistance;
                                burnersNum = burnersNumByTable;
                                lastBurnersNum = burnersNum;
                            }
                        }
                    }
                }
                minDistance = Double.MAX_VALUE;
                lastSteamCapacity = data.getSteamCapacity();
                lastMasutPresure = data.getMasutPresure();
                lastMasutConsumption = data.getMasutConsumtion();
            }
            coordinates.add(Coordinate.builder()
                    .time(data.getTime())
                    .burnersNum(burnersNum)
                    .steamCapacity(data.getSteamCapacity())
                    .boilerHouse(boilerHouse)
//                    .masutPresure(showCapacity ? data.getMasutPresure() : null)
//                    .masutConsumption(showCapacity ? data.getMasutConsumtion() : null
                    .build());
        }

        return coordinates;
    }


    public static List<Coordinate> getBurnersAmountByClusterization2(List<Data> dataList,
                                                                     BoilerHouse boilerHouse,
                                                                     Map<Integer, double[][]> idealParameters) {
        List<Coordinate> coordinates = new ArrayList<>();
        double minDistance = Double.MAX_VALUE;
        double currentDistance;
        int burnersNum = -1;
        double[][] array;
        Set<Integer> keySet;

        //Проходим по всем значениям
        for (Data dataPoint : dataList) {
            if (dataPoint.getTime().toString().equals("2023-01-02T18:50")) {
                System.out.println();
            }
            keySet = idealParameters.keySet();
            //После выполнения это цикла, мы точно можем сказать сколько горелок было включено в текузий момент
            for (int burnersNumByTable : keySet) {
                array = idealParameters.get(burnersNumByTable);
//                if (array[0][0] <= dataPoint.getSteamCapacity() && dataPoint.getSteamCapacity() <= array[0][array[0].length - 1]) {
//                    burnersNum = burnersNumByTable;
//                    break;
//                } else {
                for (int i = 0; i < array[0].length; i++) {
                    currentDistance = Math.sqrt(Math.pow(dataPoint.getMasutPresure() - array[1][i], 2)
                            + Math.pow(dataPoint.getMasutConsumtion() - array[2][i], 2)
                            + Math.pow(dataPoint.getSteamCapacity() - array[0][i], 2));
//                            currentDistance = Math.abs(100 - 100 * dataPoint.getMasutPresure() / array[1][i]) +
//                                    Math.abs(100 - 100 * dataPoint.getMasutConsumtion() / array[2][i]) +
//                                    Math.abs(100 - 100 * dataPoint.getSteamCapacity() / array[0][i]);
                    if (currentDistance < minDistance) {
                        minDistance = currentDistance;
                        burnersNum = burnersNumByTable;
                    }
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

    private static <DATA> void approximate(List<DATA> dataList, Function<DATA, LocalDateTime> dataTimeGetter,
                                           Function<DATA, Double> dataSteamCapacityGetter,
                                           BiConsumer<DATA, Double> coordinateSetter) {
        // Perform polynomial regression
        int degree = 5; // Change this to the desired polynomial degree
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
        for (int i = 0; i < coefficients.length; i++) {
            System.out.println("Coefficient of x^" + i + ": " + coefficients[i]);
        }

        for (DATA data : dataList) {
            double y = 0.0;
            double x = dataTimeGetter.apply(data).toEpochSecond(ZoneOffset.UTC);
            for (int i = 0; i < coefficients.length; i++) {
                y += coefficients[i] * Math.pow(x, i);
            }
            coordinateSetter.accept(data, y);
        }
    }

}
