package spring.buttowski.diploma.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Table(name = "boiler_house")
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoilerHouse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = true)
    private String name;

    @Column(columnDefinition = "timestamp(0)")
    private LocalDateTime maxDate;

    @Column(columnDefinition = "timestamp(0)")
    private LocalDateTime minDate;

    @OneToMany(mappedBy = "boilerHouse", cascade = CascadeType.REMOVE)
    @JsonIgnore
    private List<Data> data;

    @OneToMany(mappedBy = "boilerHouse", cascade = CascadeType.REMOVE)
    @JsonIgnore
    private List<Coordinate> coordinates;

}
