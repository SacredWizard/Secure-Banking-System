package edu.asu.sbs.repositories;

import edu.asu.sbs.models.Request;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends CrudRepository<Request, Long> {
    List<Request> findAll();

    Optional<Request> findOneByRequestId(Long requestId);
}
