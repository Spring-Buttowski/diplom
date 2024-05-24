package spring.buttowski.diploma.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BoilerUnitOperatingChart {
    private List<Integer> burnersAmount;
    private List<double[][]> parameters;
}
