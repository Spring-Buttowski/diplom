package spring.buttowski.diploma.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import spring.buttowski.diploma.models.BoilerHouse;
import spring.buttowski.diploma.models.Coordinate;
import spring.buttowski.diploma.services.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


@RestController
public class DataController {
    private final Service service;
    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    public DataController(Service service) {
        this.service = service;
    }

    @GetMapping("/coordinates-by-boiler-house")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public List<Coordinate> getDataOfBoilerHouse(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(name = "name") String boilerHouseName) {
        LocalDateTime from = LocalDateTime.parse(dateFrom, formatter);
        LocalDateTime to = LocalDateTime.parse(dateTo, formatter);
        List<Coordinate> coordinates = service.getData(from, to, boilerHouseName);
        return coordinates;
    }

    @GetMapping("/boiler-houses")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public ResponseEntity<?> getBoilerHouses() {
        List<BoilerHouse> list = service.getBoilerHouses();
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/boiler-houses/{name}")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public ResponseEntity<?> deleteBoilerHouse(@PathVariable String name) {
        service.deleteBoilerHouse(name);
        return ResponseEntity.ok("Boiler house has been deleted.");
    }

    @PostMapping("/create-boiler-house")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public ResponseEntity<?> createBoilerHouse(@RequestParam("file") MultipartFile file,
                                               @RequestParam(name = "name") String boilerHouseName,
                                               @RequestParam("idealParameters") String idealParametersJson) {
        service.saveBoilerHouse(file, boilerHouseName, idealParametersJson);
        return ResponseEntity.ok("Boiler house has been saved");
    }

}
