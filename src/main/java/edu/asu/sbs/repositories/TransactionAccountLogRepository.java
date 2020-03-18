package edu.asu.sbs.repositories;

import edu.asu.sbs.models.TransactionAccountLog;
import org.springframework.data.repository.CrudRepository;

public interface TransactionAccountLogRepository extends CrudRepository<TransactionAccountLog, Long> {
}
