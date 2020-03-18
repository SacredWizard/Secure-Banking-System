package edu.asu.sbs.services;

import com.google.common.collect.Lists;
import edu.asu.sbs.models.Transaction;
import edu.asu.sbs.repositories.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<Transaction> getTransactions() {
        List<Transaction> transactionList = Lists.newArrayList();
        transactionRepository.findAll().forEach(transactionList::add);
        return transactionList;
    }
}
