package spring.buttowski.diploma;

import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import spring.buttowski.diploma.models.BoilerHouse;
import spring.buttowski.diploma.models.Coordinate;
import spring.buttowski.diploma.repositories.BoilerHouseRepository;
import spring.buttowski.diploma.repositories.CoordinateRepository;

import java.time.ZoneOffset;
import java.util.List;

@SpringBootTest
public class BoilerHouseAnalysis {

    @Autowired
    private CoordinateRepository coordinateRepository;


    @Autowired
    private BoilerHouseRepository boilerHouseRepository;


    @Test
    public void findCofs() {
        BoilerHouse boilerHouse = boilerHouseRepository.findByName("9 котёл январь").get();
        // Example data points
        List<Coordinate> coordinates = coordinateRepository.findDataByTimeBetweenAndBoilerHouse(boilerHouse.getMinDate(), boilerHouse.getMaxDate(), boilerHouse, Sort.by("time"));

        // Add more data points as needed

        // Perform polynomial regression
        int degree = 5; // Change this to the desired polynomial degree
        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(degree);
        WeightedObservedPoints points = new WeightedObservedPoints();
        for (Coordinate coordinate : coordinates) {
            double x = coordinate.getTime().toEpochSecond(ZoneOffset.UTC);
            double y = coordinate.getSteamCapacity();
            points.add(x, y);
        }

        // Fit the polynomial to the data
        double[] coefficients = fitter.fit(points.toList());

        // Output the polynomial coefficients
        for (int i = 0; i < coefficients.length; i++) {
            System.out.println("Coefficient of x^" + i + ": " + coefficients[i]);
        }


    }
}
