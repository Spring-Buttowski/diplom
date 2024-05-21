package spring.buttowski.diploma;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import spring.buttowski.diploma.models.Data;
import spring.buttowski.diploma.util.XlsToProductListParser;

import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static spring.buttowski.diploma.util.XlsToProductListParser.formatter;

public class CSVReaderExample {
    public static void main(String[] args) {
        String csvFile = "D:\\Polytech\\4 grade\\Диплом\\diplom\\full-stack-app\\back\\src\\main\\resources\\7 котёл 2 стоба.csv";
        List<Data> dataList = new ArrayList<>();
        try (CSVReader reader = new CSVReaderBuilder(new FileReader(csvFile))
                .withCSVParser(new CSVParserBuilder().withSeparator(';').build())
                .build()) {
            String[] line;
            reader.readNext();
            reader.readNext();
            reader.readNext();
            while ((line = reader.readNext()) != null) {
                // Process the line
                if (line.length >= 4) {
                    dataList.add(Data
                            .builder()
                            .time(LocalDateTime.parse(line[0], formatter))
                            .steamCapacity(XlsToProductListParser.parseDouble(line[1]))
                            .masutConsumtion(XlsToProductListParser.parseDouble(line[2]))
                            .masutPresure(XlsToProductListParser.parseDouble(line[3]))
                            .build());
                }
            }
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }
    }
}