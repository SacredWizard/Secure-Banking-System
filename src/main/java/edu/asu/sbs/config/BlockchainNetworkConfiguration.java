package edu.asu.sbs.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.asu.sbs.fabricuser.FabricUserContext;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.NetworkConfig;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.NetworkConfigurationException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;

@Slf4j
@Configuration
public class BlockchainNetworkConfiguration {


    @Value("classpath:fabricconnections/connection-profile.json")
    private transient Resource connectionFile;

    @Bean
    public NetworkConfig createNetworkConfig() {
        NetworkConfig networkConfig = null;
        try {
            File connectionProfile = connectionFile.getFile();
            networkConfig = NetworkConfig.fromJsonFile(connectionProfile);
        } catch (IOException | InvalidArgumentException | NetworkConfigurationException ex) {
            log.error(ex.toString());
        }
        return networkConfig;
    }

    @Bean
    public HFCAClient createHFCAClient() throws Exception {

        NetworkConfig networkConfig = createNetworkConfig();
        NetworkConfig.OrgInfo clientOrg = networkConfig.getClientOrganization();
        NetworkConfig.CAInfo caInfo = clientOrg.getCertificateAuthorities().get(0);

        //Certificate authority client
        HFCAClient hfcaClient = HFCAClient.createNewInstance(caInfo);

        CryptoSuite cryptoSuite = CryptoSuite.Factory.getCryptoSuite();

        hfcaClient.setCryptoSuite(cryptoSuite);

        return hfcaClient;
    }

    @Bean(name = "AdminUserContext")
    public FabricUserContext enrollAdmin() throws Exception {

        HFCAClient hfcaClient = createHFCAClient();

        FabricUserContext adminUserContext = new FabricUserContext();
        adminUserContext.setName(BlockchainNetworkAttributes.ADMIN_NAME); // admin username
        adminUserContext.setAffiliation(BlockchainNetworkAttributes.ORG1_NAME); // affiliation
        adminUserContext.setMspId(BlockchainNetworkAttributes.ORG1_MSP); // org1 mspid
        Enrollment adminEnrollment = hfcaClient.enroll(BlockchainNetworkAttributes.ADMIN_NAME, BlockchainNetworkAttributes.ADMIN_PASSWORD); //pass admin username and password, adminpw is the default for fabric
        adminUserContext.setEnrollment(adminEnrollment);

        return adminUserContext;
    }

    @Bean
    public HFClient createHFClient() throws Exception {
        FabricUserContext userContext = enrollAdmin();
        HFClient hfClient = HFClient.createNewInstance();
        CryptoSuite cryptoSuite = CryptoSuite.Factory.getCryptoSuite();
        hfClient.setCryptoSuite(cryptoSuite);
        hfClient.setUserContext(userContext);
        return hfClient;
    }

    @Bean(name = "transactionchannel")
    public Channel createTransactionchannel() throws Exception {
        HFClient hfClient = createHFClient();
        Channel newChannel = hfClient.loadChannelFromConfig(BlockchainNetworkAttributes.CHANNEL_1_NAME, createNetworkConfig());
        if (newChannel == null) {
            throw new RuntimeException("Channel " + BlockchainNetworkAttributes.CHANNEL_1_NAME + " is not defined in the config file!");
        }

        return newChannel.initialize();
    }

    @Bean
    public ObjectMapper objectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper;
    }

}
