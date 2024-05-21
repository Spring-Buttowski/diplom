package spring.buttowski.diploma.util;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import spring.buttowski.diploma.models.BoilerHouse;
import spring.buttowski.diploma.models.Data;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Component
public class XlsToProductListParser {

    public List<Data> getProducts(MultipartFile multipartFile, BoilerHouse boilerHouse) {

//            return parseToProducts(getData(multipartFile));
        return parseCSVToProducts(multipartFile, boilerHouse);

    }

    private static final SimpleDateFormat OUTPUT_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy H:mm");
    public static final DateTimeFormatter dateformatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static Map<Integer, List<String>> getData(MultipartFile multipartFile) throws IOException, ParseException {
        Map<Integer, List<String>> data = new HashMap<>();

        try (InputStream inputStream = multipartFile.getInputStream()) {
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0);

            Iterator<Row> rowIterator = sheet.rowIterator();

            rowIterator.next();
            rowIterator.next();
            rowIterator.next();

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                List<String> rowData = new ArrayList<>();
                for (Cell cell : row) {
                    if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                        Date date = cell.getDateCellValue();
                        String formattedDate = OUTPUT_DATE_FORMAT.format(date);
                        rowData.add(formattedDate);
                    } else {
                        rowData.add(cell.toString());
                    }
                }
                data.put(row.getRowNum(), rowData);
            }
            workbook.close();
        }
        return data;
    }

    private static List<Data> parseToProducts(Map<Integer, List<String>> data) {
        List<Data> result = new ArrayList<>();
        for (List<String> dataList : data.values()) {
            if (dataList.size() >= 4) {
                result.add(Data.builder()
                        .time(LocalDateTime.parse(dataList.get(0), formatter))
                        .masutPresure(parseDouble(dataList.get(1)))
                        .masutConsumtion(parseDouble(dataList.get(2)))
                        .steamCapacity(parseDouble(dataList.get(3)))
                        .build());
            }
        }
        return result;
    }

    private static List<Data> parseCSVToProducts(MultipartFile file, BoilerHouse boilerHouse) {
        String csvFile = "D:\\Polytech\\4 grade\\Диплом\\diplom\\full-stack-app\\back\\src\\main\\resources\\7 котёл 2 стоба.csv";
        List<Data> dataList = new ArrayList<>();
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
                    dataList.add(Data
                            .builder()
                            .time(localDateTime)
                            .masutPresure(XlsToProductListParser.parseDouble(line[1]))
                            .masutConsumtion(XlsToProductListParser.parseDouble(line[2]))
                            .steamCapacity(XlsToProductListParser.parseDouble(line[3]))
                            .boilerHouse(boilerHouse)
                            .build());
                }
            }
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }
        return dataList;
    }

    private static final NumberFormat format = NumberFormat.getInstance(Locale.FRANCE);

    public static Double parseDouble(String value) {
        try {
            return format.parse(value).doubleValue();
        } catch (NumberFormatException | ParseException e) {
            return 0.0;
        }
    }
}
