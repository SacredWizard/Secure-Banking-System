package edu.asu.sbs.repositories;

import edu.asu.sbs.models.Session;
import org.springframework.data.repository.CrudRepository;

public interface SessionRepository extends CrudRepository<Session,Long> {
}
