package spring.buttowski.diploma.services;

import spring.buttowski.diploma.models.Coordinate;
import spring.buttowski.diploma.models.Data;
import spring.buttowski.diploma.repositories.DataRepository;
import spring.buttowski.diploma.util.BurnerNumberDetermination;
import spring.buttowski.diploma.util.XlsToProductListParser;
import org.apache.poi.util.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;

@Service
public class DataService {
    private final DataRepository dataRepository;
    private final BurnerNumberDetermination burnerNumberDetermination;
    private final XlsToProductListParser productListParser;

    @Autowired
    public DataService(DataRepository dataRepository, BurnerNumberDetermination burnerNumberDetermination, XlsToProductListParser productListParser) {
        this.dataRepository = dataRepository;
        this.burnerNumberDetermination = burnerNumberDetermination;
        this.productListParser = productListParser;
    }

    public List<Coordinate> getData(LocalDateTime dateFrom, LocalDateTime dateTo, boolean additional) {
        List<Data> dataList = dataRepository.findDataByTimeBetween(dateFrom, dateTo);
        approximate(dataList, 50);
        List<Coordinate> coordinates = burnerNumberDetermination.getBurnersAmountByClusterization(dataList, additional);

        int previousBurnersNum;
        int currentBurnersNum;
        int counter = 0;
        LocalDateTime previousTime;

        Iterator<Coordinate> iterator = coordinates.iterator();
        Coordinate first = iterator.next();
        previousBurnersNum = first.getBurnersNum() - 1;
        previousTime = first.getTime();

        while (iterator.hasNext()) {
            Coordinate coordinate = iterator.next();
            currentBurnersNum = coordinate.getBurnersNum();
            while (currentBurnersNum == previousBurnersNum) {
                if (!iterator.hasNext()) break;
                coordinate = iterator.next();
                currentBurnersNum = coordinate.getBurnersNum();
                counter++;
            }
            if (counter < 60) {
                System.out.println(previousTime + " - " + coordinate.getTime().minusMinutes(1));
            }
            counter = 0;
            previousBurnersNum = currentBurnersNum;
            previousTime = coordinate.getTime();
        }
        return coordinates;
    }

    private void approximate(List<Data> dataList, int border) {
        Data[] dataArray = dataList.toArray(new Data[0]);
        double sumSteamCapacity = 0.0;
        double sumMasutPresure = 0.0;
        double sumMasutConsumption = 0.0;
        for (int outerIndex = 0; outerIndex < dataList.size(); outerIndex++) {
            for (int innerIndex = Math.max(0, outerIndex - border);
                 innerIndex < Math.min(dataList.size(), outerIndex + border);
                 innerIndex++) {
                sumSteamCapacity += dataArray[innerIndex].getSteamCapacity();
                sumMasutConsumption += dataArray[innerIndex].getMasutConsumtion();
                sumMasutPresure += dataArray[innerIndex].getMasutPresure();
            }
            //equals outerIndex in case it between 0 and 30
            int leftQuantity = Math.min(outerIndex, border);
            //equals outerIndex in case it between dataList.size()-30 and dataList.size()
            int rightQuantity = border;
            if (outerIndex > dataList.size() - border) {
                rightQuantity = dataList.size() - outerIndex;
            }
            int quantity = leftQuantity + rightQuantity;
            dataList.get(outerIndex).setSteamCapacity(sumSteamCapacity / quantity);
            dataList.get(outerIndex).setMasutPresure(sumMasutPresure / quantity);
            dataList.get(outerIndex).setMasutConsumtion(sumMasutConsumption / quantity);
            sumSteamCapacity = 0.0;
            sumMasutConsumption = 0.0;
            sumMasutPresure = 0.0;
        }
    }

    public void saveFileData(MultipartFile file) {
        IOUtils.setByteArrayMaxOverride(Integer.MAX_VALUE);
        List<Data> dataList = productListParser.getProducts(file);
        dataRepository.saveAll(dataList);
    }
}
