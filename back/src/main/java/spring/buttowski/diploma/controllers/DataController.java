package spring.buttowski.diploma.controllers;

import spring.buttowski.diploma.models.Coordinate;
import spring.buttowski.diploma.services.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


//fetch('http://localhost:8080/coordinates?dateFrom=06.01.2022 05:01:00&dateTo=06.01.2022 05:15:00')


@RestController
public class DataController {
    private final DataService dataService;
    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");

    @Autowired
    public DataController(DataService dataService) {
        this.dataService = dataService;
    }

    @GetMapping("/coordinates")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public List<Coordinate> getData(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam Boolean consumptionIsShown) {
        LocalDateTime from = LocalDateTime.parse(dateFrom, formatter);
        LocalDateTime to = LocalDateTime.parse(dateTo, formatter);

        List<Coordinate> coordinates = dataService.getData(from, to, consumptionIsShown);
        return coordinates;
    }


    @PostMapping("/send")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public void send(@RequestParam("file") MultipartFile file) {
        dataService.saveFileData(file);
    }

}
