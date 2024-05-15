package spring.buttowski.diploma;

import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.classifiers.trees.RandomForest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import spring.buttowski.diploma.models.Coordinate;
import spring.buttowski.diploma.models.Data;
import spring.buttowski.diploma.repositories.DataRepository;
import spring.buttowski.diploma.util.BurnerNumberDetermination;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static spring.buttowski.diploma.services.DataService.countGaps;
import static spring.buttowski.diploma.services.DataService.movingAverage;


@SpringBootTest
class DiplomaApplicationTests {
    private final DataRepository dataRepository;
    LocalDateTime dateFrom = LocalDateTime.of(2022, 1, 1, 1, 1);
    LocalDateTime dateTo = LocalDateTime.of(2022, 12, 31, 1, 1);

    @Autowired
    DiplomaApplicationTests(DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Test
    public void findOptimalWindow() {

        for (int border = 2; border <= 200; border++) {
            System.out.print(border + ",");
            //Находим все нужные нам значения параметров работы за определённый промежуток времени
            List<Data> dataList = dataRepository.findDataByTimeBetween(dateFrom, dateTo);

            //"Сглаживаем" данные методом кользящего среднего
            movingAverage(dataList, border);

            //Считаем кол-во включенных горелок для каждого момента времени
            List<Coordinate> coordinates = BurnerNumberDetermination.getBurnersAmountByClusterization(dataList, false);

            //Печатаем все промежутки времени
            countGaps(coordinates);
        }
    }

    @Test
    public void makeData() throws FileNotFoundException {
        //Находим все нужные нам значения параметров работы за определённый промежуток времени
        List<Data> dataList = dataRepository.findDataByTimeBetween(dateFrom, dateTo);

        //"Сглаживаем" данные методом кользящего среднего
        movingAverage(dataList, 100);

        //Считаем кол-во включенных горелок для каждого момента времени
        List<Coordinate> coordinates = BurnerNumberDetermination.getBurnersAmountByClusterization(dataList, true);

        //Печатаем все промежутки времени
//        countGaps(coordinates);

        //Печатаем данные для обучения в файл
        String pathValue = "D:\\Polytech\\4 grade\\Диплом\\diplom\\full-stack-app\\back\\src\\main\\resources\\data.arff";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(pathValue))) {
            for (Coordinate coordinate : coordinates) {
                writer.write(String.format("%.10f", coordinate.getSteamCapacity()) + " " +
                        String.format("%.10f", coordinate.getMasutPresure()) + " "
                        + String.format("%.10f", coordinate.getMasutConsumption()) + " "
                        + coordinate.getBurnersNum() + "\n");
            }
        } catch (
                IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    public void machineLearning() throws Exception {
        // Load data
        DataSource source = new DataSource("D:\\Polytech\\4 grade\\Диплом\\diplom\\full-stack-app\\back\\src\\main\\resources\\data.arff");
        Instances trainData = source.getDataSet();

        // Set class index to the number of burners
        trainData.setClassIndex(trainData.numAttributes() - 1);

        // Create a model
        RandomForest model = new RandomForest();
        model.buildClassifier(trainData);

        // Save the model
        weka.core.SerializationHelper.write("randomForest.model", model);

        // Load model and make predictions
        RandomForest loadedModel = (RandomForest) weka.core.SerializationHelper.read("randomForest.model");
        Instances newData = source.getStructure();
        newData.setClassIndex(newData.numAttributes() - 1);

        double label = loadedModel.classifyInstance(newData.instance(0));
        System.out.println("Predicted number of burners: " + label);
    }


}
