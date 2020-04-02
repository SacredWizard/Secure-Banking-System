package edu.asu.sbs.repositories;

import edu.asu.sbs.models.Transaction;
import edu.asu.sbs.util.RichQuery;

import java.util.List;

public interface TransactionHyperledgerRepository {
    Transaction getById(Long id);

    void save(Transaction fish);

    List<Transaction> query(RichQuery query);

    void delete(Long id);

    List<Transaction> getAll();
}
