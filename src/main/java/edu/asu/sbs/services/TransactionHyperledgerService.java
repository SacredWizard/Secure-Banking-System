package edu.asu.sbs.services;

import edu.asu.sbs.models.Transaction;
import edu.asu.sbs.repositories.TransactionHyperledgerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionHyperledgerService {

    private final TransactionHyperledgerRepository transactionHyperledgerRepository;

    public TransactionHyperledgerService(TransactionHyperledgerRepository transactionHyperledgerRepository) {
        this.transactionHyperledgerRepository = transactionHyperledgerRepository;
    }

    public Transaction getById(Long id) {
        return transactionHyperledgerRepository.getById(id);
    }

    public void save(Transaction transaction) {
        transactionHyperledgerRepository.save(transaction);
    }

    public void delete(Long id) {
        transactionHyperledgerRepository.delete(id);
    }

    public List<Transaction> getAll() {
        return transactionHyperledgerRepository.getAll();
    }
}
