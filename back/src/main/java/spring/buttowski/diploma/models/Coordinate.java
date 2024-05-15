package spring.buttowski.diploma.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Coordinate {
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime time;
    private int burnersNum;
    private Double steamCapacity;
    private Double masutPresure;
    private Double masutConsumption;

}
