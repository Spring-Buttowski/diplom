package spring.buttowski.diploma.repositories;

import org.apache.poi.sl.draw.geom.GuideIf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import spring.buttowski.diploma.models.BoilerHouse;

import java.util.Optional;

@Repository
public interface BoilerHouseRepository extends JpaRepository<BoilerHouse, Integer> {
    Optional<BoilerHouse> findByName(String name);

    void deleteByName(String name);
}
