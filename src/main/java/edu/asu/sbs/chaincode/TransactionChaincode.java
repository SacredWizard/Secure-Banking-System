package edu.asu.sbs.chaincode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.Maps;
import org.apache.commons.compress.utils.Lists;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.*;
import org.hyperledger.fabric.shim.Chaincode;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.KeyValue;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.hyperledger.fabric.shim.ResponseUtils.newSuccessResponse;


@Contract(name = "TransactionChaincode",
        info = @Info(title = "Transaction Chaincode for SBS Bank",
                description = "Transaction Chaincode for SBS Bank",
                version = "1",
                license
                        = @License(name = "SPDX-License-Identifier: Apache-2.0",
                        url = ""),
                contact = @Contact(email = "asecurebank@gmail.com",
                        name = "TransactionChaincode",
                        url = "http://sbsbank.xyz")))
@Default
public class TransactionChaincode implements ContractInterface {
    //    peer lifecycle chaincode package banking.tar.gz --path ${CC_SRC_PATH} --lang ${CC_RUNTIME_LANGUAGE} --label banking_${VERSION} >&log.txt
    public TransactionChaincode() {

    }

    @Transaction()
    public void initLedger(final Context ctx) {
        ChaincodeStub chaincodeStub = ctx.getStub();
        chaincodeStub.putStringState("a", "ASASAS");
    }

    @Transaction
    public String get(Context ctx, String key) {

        String value = ctx.getStub().getStringState(key);
        if (value == null || value.isEmpty()) {
            return null;
        }
        return value;
    }

    @Transaction
    public List<String> query(Context ctx, String query) {
        List<String> transactionList = Lists.newArrayList();
        //key value pair result iterator
        for (KeyValue keyValue : ctx.getStub().getQueryResult(query)) {
            String key = keyValue.getKey();
            String transaction = get(ctx, key);
            transactionList.add(transaction);
        }
        return transactionList;
    }

    @Transaction
    public String set(Context ctx, String key, String value) {
        ctx.getStub().putStringState(key, value);
        return "Successfully set key : " + key + " as value : " + value;
    }

    @Transaction
    public String delete(Context ctx, String key) {

        // Delete the key from the state in ledger
        ctx.getStub().delState(key);
        return "Successfully deleted key : " + key + "from the ledger";
    }

    @Transaction
    public String getHistory(Context ctx, String key) {
        String payload = "";
        List<Map<String, Object>> historyList = Lists.newArrayList();

        //key value pair result iterator
        Iterator<KeyModification> iterator = ctx.getStub().getHistoryForKey(key).iterator();
        if (!iterator.hasNext()) {
            return "[]";
        }
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModules(new JavaTimeModule());
        while (iterator.hasNext()) {
            HashMap<String, Object> history = Maps.newHashMap();
            KeyModification modification = iterator.next();
            history.put("asset", modification.getStringValue());
            history.put("transactionId", modification.getTxId());
            history.put("timeStamp", modification.getTimestamp());
            historyList.add(history);
        }
        try {
            payload = objectMapper.writeValueAsString(historyList);
        } catch (JsonProcessingException ex) {
            ex.printStackTrace();
        }

        Chaincode.Response response = newSuccessResponse("Query succesful", payload.getBytes(StandardCharsets.UTF_8));
        return response.getStringPayload();
    }


}
