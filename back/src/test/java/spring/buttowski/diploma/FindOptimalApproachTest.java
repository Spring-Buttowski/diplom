package spring.buttowski.diploma;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import spring.buttowski.diploma.models.BoilerHouse;
import spring.buttowski.diploma.models.Coordinate;
import spring.buttowski.diploma.models.RawData;
import spring.buttowski.diploma.repositories.BoilerHouseRepository;
import spring.buttowski.diploma.repositories.DataRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static spring.buttowski.diploma.services.Service.approximate;
import static spring.buttowski.diploma.services.Service.countGaps;
import static spring.buttowski.diploma.services.Service.getBurnersAmountByClusterization;
import static spring.buttowski.diploma.services.Service.movingAverage;


@SpringBootTest
class FindOptimalApproachTest {
    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private BoilerHouseRepository boilerHouseRepository;

    private final Map<Integer, double[][]> idealParameters = new HashMap<>();

    {
        idealParameters.put(4, new double[][]{
                {30.5, 34., 36., 38.1},
                {13.5, 16.5, 19.5, 21.2},
                {2.75, 3.05, 3.25, 3.38}
        });
        idealParameters.put(5, new double[][]{
                {41., 42.5, 46.},
                {14.5, 16., 19.8},
                {3.62, 3.78, 4.25}
        });
        idealParameters.put(6, new double[][]{
                {48., 50.},
                {15.2, 16.5},
                {4.45, 4.62}
        });
    }

    @Test
    public void findOptimalWindow() {
        BoilerHouse boilerHouse = boilerHouseRepository.findByName("7 котёл 2022 DEFAULT").get();
        for (int border = 2; border <= 200; border+=10) {
            //Находим все нужные нам значения параметров работы за определённый промежуток времени
            List<RawData> rawDataList = dataRepository.findDataByTimeBetweenAndBoilerHouse(boilerHouse.getMinDate(), boilerHouse.getMaxDate(), boilerHouse);

            //"Сглаживаем" данные методом кользящего среднего
            movingAverage(rawDataList, border);

            //Считаем кол-во включенных горелок для каждого момента времени
            List<Coordinate> coordinates = getBurnersAmountByClusterization(rawDataList, boilerHouse, idealParameters);

            //Печатаем все промежутки времени
            int count = countGaps(coordinates);
            System.out.println(border + "," + count);
        }
    }


    @Test
    public void findOptimalApproximation() {
        BoilerHouse boilerHouse = boilerHouseRepository.findByName("7 котёл 2022 DEFAULT").get();
        int maxGap = 500;
        int maxDegree = 5;
        int minGap = 50;
        int gapStep = 50;

        int gapSteps = (maxGap - minGap) / gapStep + 1;
        int degreeSteps = maxDegree - 2 + 1;

        int[][] implicitGapsTable = new int[gapSteps][degreeSteps];

        for (int gapIndex = 0, gap = minGap; gap <= maxGap; gapIndex++, gap += gapStep) {
            for (int degree = 2; degree <= maxDegree; degree++) {
                List<RawData> rawDataList = dataRepository.findDataByTimeBetweenAndBoilerHouse(boilerHouse.getMinDate(), boilerHouse.getMaxDate(), boilerHouse);

                for (int start = 0, end = gap; end < rawDataList.size(); start += gap, end += gap) {
                    approximate(rawDataList.subList(start, end), RawData::getTime, RawData::getFuelOilPressure, RawData::setFuelOilPressure, degree);
                    approximate(rawDataList.subList(start, end), RawData::getTime, RawData::getFuelOilConsumption, RawData::setFuelOilConsumption, degree);
                    approximate(rawDataList.subList(start, end), RawData::getTime, RawData::getSteamCapacity, RawData::setSteamCapacity, degree);
                }

                List<Coordinate> coordinates = getBurnersAmountByClusterization(rawDataList, boilerHouse, idealParameters);

                int implicitGapsCounter = countGaps(coordinates);

                implicitGapsTable[gapIndex][degree - 2] = implicitGapsCounter;
            }
        }

        System.out.println("Gap\\Degree");
        System.out.print("     ");
        for (int degree = 2; degree <= maxDegree; degree++) {
            System.out.printf("%8d", degree);
        }
        System.out.println();

        for (int gapIndex = 0, gap = minGap; gap <= maxGap; gapIndex++, gap += gapStep) {
            System.out.printf("%4d ", gap);
            for (int degree = 2; degree <= maxDegree; degree++) {
                System.out.printf("%8d", implicitGapsTable[gapIndex][degree - 2]);
            }
            System.out.println();
        }
    }

}
