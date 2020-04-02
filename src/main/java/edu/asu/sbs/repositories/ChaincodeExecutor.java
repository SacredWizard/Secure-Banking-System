package edu.asu.sbs.repositories;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.asu.sbs.config.BlockchainNetworkAttributes;
import edu.asu.sbs.util.RichQuery;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.ServiceDiscoveryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class ChaincodeExecutor {

    private final long waitTime = 6000; //Milliseconds
    private ChaincodeID ccId;
    @Autowired
    @Qualifier("transactionchannel")
    private Channel channel;

    @Autowired
    private HFClient hfClient;

    @Autowired
    private ObjectMapper objectMapper;

//    public ChaincodeExecutor(Channel channel, HFClient hfClient, ObjectMapper objectMapper) {
//        this.channel = channel;
//        this.hfClient = hfClient;
//        this.objectMapper = objectMapper;
//    }

    public String executeTransaction(boolean invoke, String func, String... args) throws InvalidArgumentException, ProposalException, InterruptedException, ExecutionException, TimeoutException, ServiceDiscoveryException {


        ChaincodeID.Builder chaincodeIDBuilder = ChaincodeID.newBuilder()
                .setName(BlockchainNetworkAttributes.CHAINCODE_1_NAME)
                .setVersion(BlockchainNetworkAttributes.CHAINCODE_1_VERSION);
        ccId = chaincodeIDBuilder.build();

        TransactionProposalRequest transactionProposalRequest = hfClient.newTransactionProposalRequest();
        transactionProposalRequest.setChaincodeID(ccId);
        transactionProposalRequest.setChaincodeLanguage(TransactionRequest.Type.JAVA);

        transactionProposalRequest.setFcn(func);
        transactionProposalRequest.setArgs(args);
        transactionProposalRequest.setProposalWaitTime(waitTime);
        String payload = "";

        List<ProposalResponse> successful = Lists.newArrayList();
        List<ProposalResponse> failed = Lists.newArrayList();

        // Java sdk will send transaction proposal to all peers, if some peer down but the response still meet the endorsement policy of chaincode,
        // there is no need to retry. If not, you should re-send the transaction proposal.
        log.info("Sending transactionproposal to chaincode: function = '{}' args = '{}'", func, String.join(" , ", args));
        Channel.DiscoveryOptions discoveryOptions = new Channel.DiscoveryOptions();
        discoveryOptions.setForceDiscovery(true);
        Collection<ProposalResponse> transactionPropResp = channel.sendTransactionProposalToEndorsers(transactionProposalRequest, discoveryOptions);

        log.info(transactionProposalRequest.toString());
        log.info(transactionPropResp.toString());

        for (ProposalResponse response : transactionPropResp) {

            if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                payload = new String(response.getChaincodeActionResponsePayload());
                log.info("[√] Got success response from peer '{}'  => Message : '{}' Payload: '{}'", response.getPeer().getName(), response.getMessage(), payload);
                successful.add(response);
            } else {
                String status = response.getStatus().toString();
                String msg = response.getMessage();
                log.info("[×] Got failed response from peer " + response.getPeer().getName() + " => Message : " + msg + " Status :" + status);
                failed.add(response);
            }
        }

        if (invoke) {
            log.info("Sending transaction to orderers...");
            // Java sdk tries all orderers to send transaction, so don't worry about one orderer gone.
            try {
                CompletableFuture<BlockEvent.TransactionEvent> future = channel.sendTransaction(successful);
                if (future.isDone()) {
                    BlockEvent.TransactionEvent transactionEvent = future.get();
                    log.info("Orderer response: txid: " + transactionEvent.getTransactionID());
                    log.info("Orderer response: block number: " + transactionEvent.getBlockEvent().getBlockNumber());
                    return null;
                }
            } catch (InterruptedException | ExecutionException ex) {
                log.error("Orderer exception happened: " + ex);
                return null;
            }

        }
        return payload;
    }

    public String saveObject(String key, String json) {

        String result = "";
        String[] args = {key, json};
        try {
            result = executeTransaction(true, "set", args);
        } catch (InvalidArgumentException | ProposalException | InterruptedException | ExecutionException | TimeoutException | ServiceDiscoveryException ex) {
            log.error(ex.toString());
        }

        return result;
    }

    public String getObjectByKey(String key) {
        String result = "";
        try {
            result = executeTransaction(false, "get", key);
        } catch (InvalidArgumentException | ProposalException | InterruptedException | ExecutionException | TimeoutException | ServiceDiscoveryException ex) {
            log.error(ex.toString());
        }

        return result;
    }

    public String deleteObject(String key) {
        String result = "";
        try {
            result = executeTransaction(true, "delete", key);
        } catch (InvalidArgumentException | ProposalException | InterruptedException | ExecutionException | TimeoutException | ServiceDiscoveryException ex) {
            log.error(ex.toString());
        }

        return result;
    }

    public String query(RichQuery query) {
        String result = "";
        try {
//            String[] args = {objectMapper.writeValueAsString(query)};
            String[] args = {};
            result = executeTransaction(false, "queryAllTransactions", args);
        } catch (InvalidArgumentException | ProposalException | InterruptedException | ExecutionException | TimeoutException | ServiceDiscoveryException ex) {
            log.error(ex.toString());
        }
        return result;
    }

    public String getObjectHistory(String key) {
        String result = "";
        try {
            result = executeTransaction(false, "getHistory", key);
        } catch (InvalidArgumentException | ProposalException | InterruptedException | ExecutionException | TimeoutException | ServiceDiscoveryException ex) {
            log.error(ex.toString());
        }

        return result;
    }

}
