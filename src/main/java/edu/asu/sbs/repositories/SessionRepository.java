package edu.asu.sbs.repositories;

import edu.asu.sbs.models.Session;
import edu.asu.sbs.models.User;
import org.springframework.data.repository.CrudRepository;

public interface SessionRepository extends CrudRepository<Session,Long> {
    Session findByLinkedUser(User currentUser);
}
