package spring.buttowski.diploma.repositories;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.buttowski.diploma.models.BoilerHouse;
import spring.buttowski.diploma.models.Coordinate;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CoordinateRepository extends JpaRepository<Coordinate, LocalDateTime> {
    List<Coordinate> findDataByTimeBetweenAndBoilerHouse(LocalDateTime start, LocalDateTime end, BoilerHouse boilerHouse, Sort sort);
}
