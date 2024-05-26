package spring.buttowski.diploma.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "raw_data")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "time", columnDefinition = "timestamp(0)")
    private LocalDateTime time;

    @Column(name = "fuel_oil_pressure")
    private double fuelOilPressure;

    @Column(name = "fuel_oil_consumption")
    private double fuelOilConsumption;

    @Column(name = "steam_capacity")
    private double steamCapacity;

    @ManyToOne
    @JoinColumn(name = "boiler_house_id", referencedColumnName = "id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private BoilerHouse boilerHouse;
}
