package edu.asu.sbs.repositories;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.Maps;
import edu.asu.sbs.models.Transaction;
import edu.asu.sbs.util.RichQuery;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class TransactionHyperledgerRepositoryImpl implements TransactionHyperledgerRepository {

    private final ChaincodeExecutor chaincodeExecutor;
    private final ObjectMapper objectMapper;

    public TransactionHyperledgerRepositoryImpl(ChaincodeExecutor chaincodeExecutor, ObjectMapper objectMapper) {
        this.chaincodeExecutor = chaincodeExecutor;
        this.objectMapper = objectMapper;
        JavaTimeModule module = new JavaTimeModule();
        objectMapper.registerModule(module);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Override
    public String getById(Long id) {
        String key = String.valueOf(id);
        String json = chaincodeExecutor.getObjectByKey(key);
        return json;
//        Transaction transaction = null;
//        if (json != null && !json.isEmpty()) {
//            try {
//                transaction = objectMapper.readValue(json, Transaction.class);
//            } catch (IOException ex) {
//                log.error(ex.toString());
//            }
//        }
//        return transaction;
    }

    @Override
    public void save(Transaction transaction) {
        String json = "";
        try {
            json = objectMapper.writeValueAsString(transaction);
        } catch (JsonProcessingException ex) {
            log.error(ex.toString());
        }

        chaincodeExecutor.saveObject(String.valueOf(transaction.getTransactionId()), json);
    }

    @Override
    public List<Transaction> query(RichQuery query) {
        List<Transaction> transactionList = Lists.newArrayList();
        TypeReference<List<Transaction>> listType = new TypeReference<List<Transaction>>() {
        };

        String json = chaincodeExecutor.query(query);

        try {
            transactionList = objectMapper.readValue(json, listType);
        } catch (IOException ex) {
            log.error(ex.toString());
        }

        return transactionList;
    }

    @Override
    public void delete(Long id) {
        chaincodeExecutor.deleteObject(String.valueOf(id));
    }

    @Override
    public String getAll() {
        List<Transaction> transactionList = Lists.newArrayList();
        TypeReference<List<Transaction>> listType = new TypeReference<List<Transaction>>() {
        };

        RichQuery query = new RichQuery();
        Map<String, Object> selector = Maps.newHashMap();
        selector.put("docType", "transaction");
        query.setSelector(selector);

        String json = chaincodeExecutor.query(query);
        return json;
//        try {
//
//            transactionList = objectMapper.readValue(json, listType);
//        } catch (IOException ex) {
//            log.error(ex.toString());
//        }
//
//        return transactionList;
    }

    @Override
    public String getHistory(String id) {
        return chaincodeExecutor.getObjectHistory(id);
    }

}
