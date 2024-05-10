package spring.buttowski.diploma.util;

import spring.buttowski.diploma.models.Data;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class XlsToProductListParser {

    public List<Data> getProducts(MultipartFile multipartFile) {
        try {
            return parseToProducts(getData(multipartFile));
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private static final SimpleDateFormat OUTPUT_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy H:mm:ss");

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
            if (dataList.size() == 4) {
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

    private static Double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
