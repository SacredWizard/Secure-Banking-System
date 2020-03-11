package edu.asu.sbs.repositories;

import edu.asu.sbs.models.Request;
import org.springframework.data.repository.CrudRepository;

public interface RequestRepository extends CrudRepository<Request,Long> {
}
