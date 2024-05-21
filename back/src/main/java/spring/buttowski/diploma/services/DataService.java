package spring.buttowski.diploma.services;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import spring.buttowski.diploma.controllers.DataController;
import spring.buttowski.diploma.models.BoilerHouse;
import spring.buttowski.diploma.models.Coordinate;
import spring.buttowski.diploma.models.Data;
import spring.buttowski.diploma.repositories.BoilerHouseRepository;
import spring.buttowski.diploma.repositories.CoordinateRepository;
import spring.buttowski.diploma.repositories.DataRepository;
import spring.buttowski.diploma.util.BurnerNumberDetermination;
import spring.buttowski.diploma.util.XlsToProductListParser;
import org.apache.poi.util.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Service
@Slf4j
public class DataService {
    private final DataRepository dataRepository;
    private final BurnerNumberDetermination burnerNumberDetermination;
    private final XlsToProductListParser productListParser;
    private final BoilerHouseRepository boilerHouseRepository;
    private final CoordinateRepository coordinateRepository;

    @Autowired
    public DataService(DataRepository dataRepository, BurnerNumberDetermination burnerNumberDetermination, XlsToProductListParser productListParser, BoilerHouseRepository boilerHouseRepository, CoordinateRepository coordinateRepository) {
        this.dataRepository = dataRepository;
        this.burnerNumberDetermination = burnerNumberDetermination;
        this.productListParser = productListParser;
        this.boilerHouseRepository = boilerHouseRepository;
        this.coordinateRepository = coordinateRepository;
    }

    public List<Coordinate> getData(LocalDateTime dateFrom, LocalDateTime dateTo, String boilerHouseName) {
        BoilerHouse boilerHouse = boilerHouseRepository.findByName(boilerHouseName).get();
        //Находим все нужные нам значения параметров работы за определённый промежуток времени
        List<Coordinate> coordinates = coordinateRepository.findDataByTimeBetweenAndBoilerHouse(dateFrom, dateTo, boilerHouse);
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
            System.out.println(gapStart.getTime().format(DataController.formatter) +
                    " - " + next.getTime().minusMinutes(1).format(DataController.formatter)
                    + " " + gapStart.getBurnersNum() + " " + alarm);
        }
        System.out.println("-------------(Implicit gaps' amount - " + implicitGapsCounter + ")");
//        System.out.printlnеу(implicitGapsCounter);
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

    public void saveBoilerHouse(MultipartFile file, String boilerHouseName) {
        IOUtils.setByteArrayMaxOverride(Integer.MAX_VALUE);
        BoilerHouse boilerHouse = boilerHouseRepository.save(BoilerHouse
                .builder()
                .name(boilerHouseName)
                .build());

        log.info("Saving data");
        List<Data> dataList = productListParser.getProducts(file, boilerHouse);
        dataRepository.saveAll(dataList);

        log.info("Evaluating coordinates");
        //"Сглаживаем" данные методом кользящего среднего
        movingAverage(dataList, 100);

        //Считаем кол-во включенных горелок для каждого момента времени
        List<Coordinate> coordinates = BurnerNumberDetermination.getBurnersAmountByClusterization(dataList, boilerHouse);

        log.info("Saving coordinates");
        coordinateRepository.saveAll(coordinates);
    }

    public List<BoilerHouse> getBoilerHouses() {
        return boilerHouseRepository.findAll().stream().filter(boilerHouse -> !boilerHouse.getData().isEmpty()).toList();
    }

    @Transactional
    public void deleteBoilerHouse(String name) {
        boilerHouseRepository.deleteByName(name);
    }
}
