package edu.asu.sbs.repositories;

import edu.asu.sbs.models.Account;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface AccountRepository extends CrudRepository<Account, Long> {
    Optional<Account> findOneByAccountNumberEquals(String s);
}
