package edu.asu.sbs.chaincode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.hyperledger.fabric.shim.ChaincodeBase;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.KeyValue;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
public class Chaincode extends ChaincodeBase {

    /**
     * Init is called when initializing or updating chaincode. Use this to set
     * initial world state
     *
     * @param stub {@link ChaincodeStub} to operate proposal and ledger
     * @return Response with message and payload
     */
    @Override
    public Response init(ChaincodeStub stub) {

        return newSuccessResponse();
    }

    /**
     * Invoke is called to read from or write to the ledger
     *
     * @param stub {@link ChaincodeStub} to operate proposal and ledger
     * @return Response
     */
    @Override
    public Response invoke(ChaincodeStub stub) {
        try {
            // Extract the function and args from the transaction proposal
            String func = stub.getFunction();
            List<String> params = stub.getParameters();
            switch (func) {
                case "set":
                    // Return result as success payload
                    return set(stub, params);
                case "get":
                    // Return result as success payload
                    return get(stub, params);
                case "delete":
                    // Return result as success payload
                    return delete(stub, params);
                case "query":
                    // Return result as success payload
                    return query(stub, params);
                case "getHistory":
                    // Return result as success payload
                    return getHistory(stub, params);
                default:
                    break;
            }
            //Error if unknown method
            return ChaincodeBase.newErrorResponse("Invalid invoke function name. Expecting one of: [\"set\", \"get\", \"delete\", \"query\", \"getHistory\"");
        } catch (Throwable e) {
            return ChaincodeBase.newErrorResponse(e.getMessage());
        }
    }

    /**
     * get receives the value of a key from the ledger
     *
     * @param stub {@link ChaincodeStub} to operate proposal and ledger
     * @param args key
     * @return Response with message and payload
     */
    private Response get(ChaincodeStub stub, List<String> args) {
        if (args.size() != 1) {
            return newErrorResponse("Incorrect arguments. Expecting a key");
        }

        String value = stub.getStringState(args.get(0));
        if (value == null || value.isEmpty()) {
            return newErrorResponse("Asset not found with key: " + args.get(0));
        }
        Response response = newSuccessResponse("Returned value for key : " + args.get(0) + " = " + value, value.getBytes(StandardCharsets.UTF_8));
        return response;
    }

    /**
     * Rich query using json to read from world state
     *
     * @param stub {@link ChaincodeStub} to operate proposal and ledger
     * @param args json query
     * @return Response with message and payload
     */
    private Response query(ChaincodeStub stub, List<String> args) {
        String payload = "";

        //key value pair result iterator
        Iterator<KeyValue> iterator = stub.getQueryResult(args.get(0)).iterator();
        if (!iterator.hasNext()) {
            return newSuccessResponse("No results", "[]".getBytes(StandardCharsets.UTF_8));
        }
        while (iterator.hasNext()) {
            payload += iterator.next().getStringValue() + ",";
        }
        payload = payload.substring(0, payload.length() - 1);
        payload = "[" + payload + "]";

        Response response = newSuccessResponse("Query succesful", payload.getBytes(StandardCharsets.UTF_8));

        return response;
    }

    /**
     * set stores the asset (both key and value) on the ledger. If the key
     * exists, it will override the value with the new one
     *
     * @param stub {@link ChaincodeStub} to operate proposal and ledger
     * @param args key and value
     * @return value
     */
    private Response set(ChaincodeStub stub, List<String> args) {
        if (args.size() != 2) {
            return newErrorResponse("Incorrect arguments. Expecting a key and a value");
        }
        stub.putStringState(args.get(0), args.get(1));
        return newSuccessResponse("Succesfully set key : " + args.get(0) + " as value : " + args.get(1), args.get(1).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Delete the key from the state in ledger
     *
     * @param stub {@link ChaincodeStub} to operate proposal and ledger
     * @param args key
     * @return Response with message and payload
     */
    private Response delete(ChaincodeStub stub, List<String> args) {
        if (args.size() != 1) {
            return newErrorResponse("Incorrect number of arguments. Expecting a key");
        }
        String key = args.get(0);
        // Delete the key from the state in ledger
        stub.delState(key);
        return newSuccessResponse("Succesfully deleted key : " + args.get(0) + "from the ledger", args.get(0).getBytes(StandardCharsets.UTF_8));
    }

//    public static void main(String[] args) {
//
//        new Chaincode().start(args);
//    }

    /**
     * getHistory returns all transactions for an object by its key This does
     * not include read only operations (which don't use a transaction!)
     *
     * @param stub {@link ChaincodeStub} to operate proposal and ledger
     * @param args key
     * @return Response with message and payload
     */
    private Response getHistory(ChaincodeStub stub, List<String> args) {
        String payload = "";
        List<Map<String, Object>> historyList = Lists.newArrayList();

        //key value pair result iterator
        Iterator<KeyModification> iterator = stub.getHistoryForKey(args.get(0)).iterator();
        if (!iterator.hasNext()) {
            return newSuccessResponse("No results", "[]".getBytes(StandardCharsets.UTF_8));
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
            log.error(ex.toString());
        }

        Response response = newSuccessResponse("Query succesful", payload.getBytes(StandardCharsets.UTF_8));
        return response;
    }

}
