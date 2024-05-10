package spring.buttowski.diploma.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "data")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Data {
    @Id
    @Column(name = "time")
    private LocalDateTime time;

    @Column(name = "masut_pressure")
    private double masutPresure;

    @Column(name = "masut_consumption")
    private double masutConsumtion;

    @Column(name = "steam_capacity")
    private double steamCapacity;
}
