package spring.buttowski.diploma.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "data")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Data {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "time", columnDefinition = "timestamp(0)")
    private LocalDateTime time;

    @Column(name = "masut_pressure")
    private double masutPresure;

    @Column(name = "masut_consumption")
    private double masutConsumtion;

    @Column(name = "steam_capacity")
    private double steamCapacity;

    @ManyToOne
    @JoinColumn(name = "boiler_house_id", referencedColumnName = "id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private BoilerHouse boilerHouse;
}
