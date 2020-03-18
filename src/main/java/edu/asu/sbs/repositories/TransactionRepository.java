package edu.asu.sbs.repositories;

import edu.asu.sbs.models.Transaction;
import org.springframework.data.repository.CrudRepository;

public interface TransactionRepository extends CrudRepository<Transaction, Long> {
}
