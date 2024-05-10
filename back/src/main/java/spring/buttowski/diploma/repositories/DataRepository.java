package spring.buttowski.diploma.repositories;

import spring.buttowski.diploma.models.Data;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DataRepository extends CrudRepository<Data, LocalDateTime> {
    List<Data> findDataByTimeBetween(LocalDateTime from, LocalDateTime to);
}
