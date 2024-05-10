package spring.buttowski.diploma.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Coordinate {
    private LocalDateTime time;
    private int burnersNum;
    private Double steamCapacity;

}
