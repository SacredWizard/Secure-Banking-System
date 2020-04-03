package edu.asu.sbs.repositories;

import edu.asu.sbs.models.Account;
import edu.asu.sbs.models.TransferRequest;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface TransferRequestRepository extends CrudRepository<TransferRequest, Long> {
    Optional <TransferRequest> findByRequestId(Long requestId);
    List<TransferRequest> findByAcceptedByAndTransferRequestStatus(Account acceptingAccount, String status);
    List<TransferRequest> findByRaisedFromAndTransferRequestStatus(Account acceptingAccount, String status);
}
