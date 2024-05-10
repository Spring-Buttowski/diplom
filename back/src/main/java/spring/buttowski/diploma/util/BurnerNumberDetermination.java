package spring.buttowski.diploma.util;

import spring.buttowski.diploma.models.Coordinate;
import spring.buttowski.diploma.models.Data;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class BurnerNumberDetermination {
    private static final Map<Integer, double[][]> idealParameters = new HashMap<>();

    static {
        idealParameters.put(4, new double[][]{
                {13.5, 16.5, 19.5, 21.2},
                {2.75, 3.05, 3.25, 3.38},
                {30.5, 34., 36., 38.1}
        });
        idealParameters.put(5, new double[][]{
                {14.5, 16., 19.8},
                {3.62, 3.78, 4.25},
                {41., 42.5, 46.}
        });
        idealParameters.put(6, new double[][]{
                {15.2, 16.5},
                {4.45, 4.62},
                {48., 50.}
        });
    }


    public List<Coordinate> getBurnersAmountByClusterization(List<Data> dataList, boolean additional) {
        List<Coordinate> coordinates = new ArrayList<>();
        double minDistance = Double.MAX_VALUE;
        double currentDistance;
        int burnersNum = -1;
        double lastSteamCapacity;
        int lastBurnersNum = 0;
        double[][] array;
        Set<Integer> keySet;
        lastSteamCapacity = dataList.get(0).getSteamCapacity();

        //Проходим по всем значениям
        for (Data data : dataList) {
//            if (data.getTime().toString().equals("2022-03-08T00:09")) {
//                System.out.println();
//            }
            //Данные с котлоагрегата могут быть сняты неправильно.
            //Если произошёл большой скачок в паропроизводительности, то в этот момент времеи занчения были сняты неправлиьно.
            //Присваиваем кол-во горелок равному на прошлой итерации
            if (Math.abs(data.getSteamCapacity() - lastSteamCapacity) > 20) {
                burnersNum = lastBurnersNum;
            } else {
                //Проверяем работал ли котлаграгет впринципе в этот момент времени
                if (Math.abs(data.getMasutPresure()) < 0.1 || Math.abs(data.getMasutConsumtion()) < 0.1 || Math.abs(data.getSteamCapacity()) < 0.1) {
                    //Котлоагргат не работал
                    burnersNum = 0;
                } else {
                    keySet = idealParameters.keySet();
                    //После выполнения это цикла, мы точно можем сказать сколько горелок было включено в текузий момент
                    for (int burnersNumByTable : keySet) {
                        array = idealParameters.get(burnersNumByTable);
                        for (int i = 0; i < array[0].length; i++) {
//                            currentDistance = Math.sqrt(Math.pow(data.getMasutPresure() - array[0][i], 2)
//                                    + Math.pow(data.getMasutConsumtion() - array[1][i], 2)
//                                    + Math.pow(data.getSteamCapacity() - array[2][i], 2));
//                            currentDistance = Math.sqrt(Math.pow((100 - 100 * data.getMasutPresure() / array[0][i]), 2)
//                                    + Math.pow((100 - 100 * data.getMasutConsumtion() / array[1][i]), 2)
//                                    + Math.pow(100 - 100 * data.getSteamCapacity() / array[2][i], 2));
                            currentDistance = Math.abs(100 - 100 * data.getMasutPresure() / array[0][i]) +
                                    Math.abs(100 - 100 * data.getMasutConsumtion() / array[1][i]) +
                                    Math.abs(100 - 100 * data.getSteamCapacity() / array[2][i]);
                            if (currentDistance < minDistance) {
                                minDistance = currentDistance;
                                burnersNum = burnersNumByTable;
                                lastBurnersNum = burnersNum;
                            }
                        }
                    }
                }
                minDistance = Double.MAX_VALUE;
                lastSteamCapacity = data.getSteamCapacity();
            }
            coordinates.add(Coordinate.builder()
                    .time(data.getTime())
                    .burnersNum(burnersNum)
                    .steamCapacity(additional ? data.getSteamCapacity() : null
//                    .steamCapacity(data.getSteamCapacity()
//                    .steamCapacity(data.getSteamCapacity()
                    ).build());
        }

        return coordinates;
    }

}
