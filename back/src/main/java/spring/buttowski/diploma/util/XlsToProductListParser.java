package spring.buttowski.diploma.util;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import spring.buttowski.diploma.models.BoilerHouse;
import spring.buttowski.diploma.models.RawData;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Component
public class XlsToProductListParser {
    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy H:mm");
    public static final DateTimeFormatter dateformatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final NumberFormat format = NumberFormat.getInstance(Locale.FRANCE);

    public List<RawData> parseCSVToProducts(MultipartFile file, BoilerHouse boilerHouse) {
        List<RawData> rawDataList = new ArrayList<>();
        try (CSVReader reader = new CSVReaderBuilder(new InputStreamReader(file.getInputStream()))
                .withCSVParser(new CSVParserBuilder().withSeparator(';').build())
                .build()) {
            String[] line;
            reader.readNext();
            reader.readNext();
            reader.readNext();
            while ((line = reader.readNext()) != null) {
                LocalDateTime localDateTime;
                try {
                    localDateTime = LocalDateTime.parse(line[0], formatter);
                } catch (DateTimeParseException e) {
                    localDateTime = LocalDateTime.of(LocalDate.parse(line[0], dateformatter), LocalTime.MIN);
                }
                // Process the line
                if (line.length >= 4) {
                    rawDataList.add(RawData
                            .builder()
                            .time(localDateTime)
                            .fuelOilPressure(XlsToProductListParser.parseDouble(line[1]))
                            .fuelOilConsumption(XlsToProductListParser.parseDouble(line[2]))
                            .steamCapacity(XlsToProductListParser.parseDouble(line[3]))
                            .boilerHouse(boilerHouse)
                            .build());
                }
            }
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }
        return rawDataList;
    }

    public static Double parseDouble(String value) {
        try {
            return format.parse(value).doubleValue();
        } catch (NumberFormatException | ParseException e) {
            return 0.0;
        }
    }
}
