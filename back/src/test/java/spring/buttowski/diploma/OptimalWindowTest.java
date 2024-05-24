package spring.buttowski.diploma;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import spring.buttowski.diploma.models.BoilerHouse;
import spring.buttowski.diploma.models.Coordinate;
import spring.buttowski.diploma.models.Data;
import spring.buttowski.diploma.repositories.BoilerHouseRepository;
import spring.buttowski.diploma.repositories.DataRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static spring.buttowski.diploma.services.DataService.countGaps;
import static spring.buttowski.diploma.services.DataService.getBurnersAmountByClusterization;
import static spring.buttowski.diploma.services.DataService.movingAverage;


@SpringBootTest
class OptimalWindowTest {
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
        BoilerHouse boilerHouse = boilerHouseRepository.findByName("7 котёл").get();
        for (int border = 2; border <= 200; border++) {
            System.out.print(border + ",");
            //Находим все нужные нам значения параметров работы за определённый промежуток времени
            List<Data> dataList = dataRepository.findDataByTimeBetweenAndBoilerHouse(boilerHouse.getMinDate(), boilerHouse.getMaxDate(), boilerHouse);

            movingAverage(dataList, 5 );
            //"Сглаживаем" данные методом кользящего среднего
            movingAverage(dataList, border);

            //Считаем кол-во включенных горелок для каждого момента времени
            List<Coordinate> coordinates = getBurnersAmountByClusterization(dataList, boilerHouse, idealParameters);

            //Печатаем все промежутки времени
            countGaps(coordinates);
        }
    }

}
