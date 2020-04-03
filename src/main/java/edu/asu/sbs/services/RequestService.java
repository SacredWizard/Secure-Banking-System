package edu.asu.sbs.services;

import com.google.common.collect.Lists;
import edu.asu.sbs.config.RequestType;
import edu.asu.sbs.config.StatusType;
import edu.asu.sbs.config.TransactionStatus;
import edu.asu.sbs.config.TransactionType;
import edu.asu.sbs.errors.GenericRuntimeException;
import edu.asu.sbs.models.*;
import edu.asu.sbs.repositories.RequestRepository;
import edu.asu.sbs.repositories.TransactionAccountLogRepository;
import edu.asu.sbs.repositories.TransactionRepository;
import edu.asu.sbs.repositories.UserRepository;
import edu.asu.sbs.repositories.*;
import edu.asu.sbs.services.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RequestService {

    private final RequestRepository requestRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionAccountLogRepository transactionAccountLogRepository;
    private final AccountService accountService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final ProfileRequestRepository profileRequestRepository;
    private final AccountRepository accountRepository;
    private final TransferRequestRepository transferRequestRepository;
    private final TransactionService transactionService;

    public RequestService(RequestRepository requestRepository, TransactionRepository transactionRepository, TransactionAccountLogRepository transactionAccountLogRepository, AccountService accountService, UserRepository userRepository, UserService userService, ProfileRequestRepository profileRequestRepository, AccountRepository accountRepository, TransferRequestRepository transferRequestRepository, TransactionService transactionService) {
        this.requestRepository = requestRepository;
        this.transactionRepository = transactionRepository;
        this.transactionAccountLogRepository = transactionAccountLogRepository;
        this.accountService = accountService;
        this.userRepository = userRepository;
        this.userService = userService;
        this.profileRequestRepository = profileRequestRepository;
        this.accountRepository = accountRepository;
        this.transferRequestRepository = transferRequestRepository;
        this.transactionService = transactionService;
    }

    public List<ProfileRequestDTO> getAllAdminRequests() {
        List<ProfileRequestDTO> RequestDTOList = Lists.newArrayList();
        List<Request> requestList = requestRepository.findByRequestTypeInAndIsDeleted(Lists.newArrayList(RequestType.TIER1_TO_TIER2, RequestType.TIER2_TO_TIER1, RequestType.UPDATE_EMP_PROFILE), false);
        for (Request request : requestList) {
            ProfileRequestDTO RequestDTO = new ProfileRequestDTO();
            RequestDTO.setEmail(request.getLinkedProfileRequest().getEmail());
            RequestDTO.setPhoneNumber(request.getLinkedProfileRequest().getPhoneNumber());
            RequestDTO.setRequestId(request.getRequestId());
            RequestDTO.setStatus(request.getStatus());
            RequestDTO.setRoleChange(request.getLinkedProfileRequest().isChangeRoleRequest());
            RequestDTO.setDescription(request.getDescription());
            RequestDTO.setCreatedDate(request.getCreatedDate());
            RequestDTO.setModifiedDate(request.getModifiedDate());
            RequestDTO.setUserId(request.getRequestBy().getId());
            RequestDTOList.add(RequestDTO);
        }
        return RequestDTOList;
    }

    public Optional<Request> getRequest(Long RequestId) {
        return requestRepository.findOneByRequestId(RequestId);
    }

    public List<Tier2RequestsDTO> getAllTier2Requests() {
        List<Tier2RequestsDTO> tier2RequestsDTOList = Lists.newArrayList();
        List<Request> requestList = requestRepository.findByRequestTypeInAndIsDeleted(Lists.newArrayList(RequestType.APPROVE_CRITICAL_TRANSACTION), false);
        for (Request request : requestList) {
            Tier2RequestsDTO tier2RequestsDTO = new Tier2RequestsDTO();
            tier2RequestsDTO.setRequestId(request.getRequestId());
            tier2RequestsDTO.setTransactionId(request.getLinkedTransaction().getTransactionId());
            tier2RequestsDTO.setStatus(request.getStatus());
            tier2RequestsDTO.setDescription(request.getDescription());
            tier2RequestsDTO.setFromAccount(request.getLinkedTransaction().getFromAccount().getId());
            tier2RequestsDTO.setToAccount(request.getLinkedTransaction().getToAccount().getId());
            tier2RequestsDTO.setAmount(request.getLinkedTransaction().getTransactionAmount());
            tier2RequestsDTO.setType(request.getLinkedTransaction().getTransactionType());
            tier2RequestsDTO.setCreatedDate(request.getCreatedDate());
            tier2RequestsDTO.setModifiedDate(request.getModifiedDate());
            tier2RequestsDTOList.add(tier2RequestsDTO);
        }
        return tier2RequestsDTOList;
    }

    @Transactional
    public void modifyRequest(Request request, User user, String requestType, String action) {
        request.setRequestType(requestType);
        request.setApprovedBy(user);
        request.setStatus(action);
        request.setModifiedDate(Instant.now());
        Transaction transaction = request.getLinkedTransaction();
        TransactionAccountLog transactionAccountLog = transaction.getLog();

        switch (action) {
            case StatusType.APPROVED:
                if (transaction.getTransactionType().equals(TransactionType.DEBIT)) {
                    transaction.getToAccount().setAccountBalance(transaction.getToAccount().getAccountBalance() + transaction.getTransactionAmount());
                } else {
                    transaction.getFromAccount().setAccountBalance(transaction.getFromAccount().getAccountBalance() + transaction.getTransactionAmount());
                }
                transaction.setStatus(StatusType.APPROVED);
                break;
            case StatusType.DECLINED:
                if (transaction.getTransactionType().equals(TransactionType.DEBIT)) {
                    transaction.getFromAccount().setAccountBalance(transaction.getFromAccount().getAccountBalance() + transaction.getTransactionAmount());
                } else {
                    transaction.getToAccount().setAccountBalance(transaction.getToAccount().getAccountBalance() + transaction.getTransactionAmount());
                }
                transaction.setStatus(StatusType.DECLINED);
                break;
            default:
                throw new GenericRuntimeException("Invalid Action");
        }
        transactionAccountLog.setLogTime(Instant.now());
        transactionAccountLog.setLogDescription(transactionAccountLog.getLogDescription() + "\n Transaction Approved on " + Instant.now());
        transactionAccountLogRepository.save(transactionAccountLog);
        transactionRepository.save(transaction);
        requestRepository.save(request);
    }

    @Transactional
    public void updateUserProfile(Request request, User approver, String requestType, String action) {

        if (request.getLinkedProfileRequest().getPhoneNumber().isEmpty() && request.getLinkedProfileRequest().getEmail().isEmpty()) {
            throw new GenericRuntimeException("Both phone number and email are empty");
        }

        request.setRequestType(requestType);
        request.setApprovedBy(approver);
        request.setStatus(action);
        request.setModifiedDate(Instant.now());
        request.setDeleted(true);
        requestRepository.save(request);

        User user = request.getRequestBy();
        switch (action) {
            case StatusType.APPROVED:
                if (!request.getLinkedProfileRequest().getPhoneNumber().isEmpty()) {
                    user.setPhoneNumber(request.getLinkedProfileRequest().getPhoneNumber());
                }
                if (!request.getLinkedProfileRequest().getEmail().isEmpty()) {
                    user.setEmail(request.getLinkedProfileRequest().getEmail());
                }
                userRepository.save(user);
                break;
            case StatusType.DECLINED:
                //don't do anything
                break;
            default:
                throw new GenericRuntimeException("Invalid Action");
        }
    }

    @Transactional
    public void updateAccountCreationRequest(Request request, User approver, String requestType, String action) {
        Account account = request.getLinkedAccount();
        request.setRequestType(requestType);
        request.setApprovedBy(approver);
        request.setStatus(action);
        request.setModifiedDate(Instant.now());
        request.setDeleted(true);
        if(StatusType.DECLINED.equals(action));
            request.setLinkedAccount(null);
        requestRepository.save(request);


        switch (action) {
            case StatusType.APPROVED:
                if(accountService.getAccountsForUser(request.getRequestBy()).size()==0)
                    account.setDefaultAccount(true);
                account.setActive(true);
                accountRepository.save(account);
                break;
            case StatusType.DECLINED:
                accountService.deleteAccount(account);
                break;
            default:
                throw new GenericRuntimeException("Invalid Action");
        }
    }

    public List<ProfileRequestDTO> getAllProfileUpdateRequests(String requestType) {
        List<ProfileRequestDTO> RequestDTOList = Lists.newArrayList();
        List<Request> requestList = requestRepository.findByRequestTypeInAndIsDeleted(Lists.newArrayList(requestType), false);
        for (Request request : requestList) {
            ProfileRequestDTO RequestDTO = new ProfileRequestDTO();
            RequestDTO.setEmail(request.getLinkedProfileRequest().getEmail());
            RequestDTO.setPhoneNumber(request.getLinkedProfileRequest().getPhoneNumber());
            RequestDTO.setRequestId(request.getRequestId());
            RequestDTO.setStatus(request.getStatus());
            RequestDTO.setDescription(request.getDescription());
            RequestDTO.setCreatedDate(request.getCreatedDate());
            RequestDTO.setModifiedDate(request.getModifiedDate());
            RequestDTO.setUserId(request.getRequestBy().getId());
            RequestDTOList.add(RequestDTO);
        }
        return RequestDTOList;
    }

    @Transactional
    public void createProfileUpdateRequest(ProfileRequestDTO requestDTO, String requestType) {
        Request request = new Request();
        request.setCreatedDate(Instant.now());
        request.setDeleted(false);
        request.setDescription(requestType);
        request.setStatus(TransactionStatus.PENDING);
        request.setRequestType(requestType);
        request.setRequestBy(userService.getCurrentUser());

        ProfileRequest profileRequest = new ProfileRequest();
        profileRequest.setRequest(request);
        profileRequest.setEmail(requestDTO.getEmail());
        profileRequest.setPhoneNumber(requestDTO.getPhoneNumber());
        request.setLinkedProfileRequest(profileRequest);
        requestRepository.save(request);
        profileRequestRepository.save(profileRequest);
    }

    @Transactional
    public void createChangeRoleRequest(String requestType) {
        Request request = new Request();
        request.setCreatedDate(Instant.now());
        request.setDeleted(false);
        request.setDescription(requestType);
        request.setStatus(TransactionStatus.PENDING);
        request.setRequestType(requestType);
        request.setRequestBy(userService.getCurrentUser());

        ProfileRequest profileRequest = new ProfileRequest();
        profileRequest.setRequest(request);
        profileRequest.setChangeRoleRequest(true);
        request.setLinkedProfileRequest(profileRequest);
        requestRepository.save(request);
        profileRequestRepository.save(profileRequest);

    }

    @Transactional
    public void updateChangeRoleRequest(Request request, String status, User approver) {
        request.setStatus(status);
        request.setApprovedBy(approver);
        request.setModifiedDate(Instant.now());
        request.setDeleted(true);
        requestRepository.save(request);
    }

    public List<DetailedNewAccountRequestDTO> getAllNewAccountRequests() {
        List<Request> newAccountRequests = requestRepository.findByRequestTypeInAndIsDeleted(Collections.singletonList(RequestType.CREATE_NEW_ACCOUNT), false);
        List<DetailedNewAccountRequestDTO> detailedNewAccountRequestDTOList = new ArrayList<>();
        for(Request newAccountRequest:newAccountRequests) {
            DetailedNewAccountRequestDTO dtoNewAccountRequest = new DetailedNewAccountRequestDTO();
            Account account = newAccountRequest.getLinkedAccount();
            dtoNewAccountRequest.setAccountNumber(account.getAccountNumber());
            dtoNewAccountRequest.setAccountType(account.getAccountType());
            dtoNewAccountRequest.setInitialDeposit(account.getAccountBalance());
            dtoNewAccountRequest.setUserName(account.getUser().getUserName());
            dtoNewAccountRequest.setRequestId(newAccountRequest.getRequestId());
            detailedNewAccountRequestDTOList.add(dtoNewAccountRequest);

        }
        return detailedNewAccountRequestDTOList;
    }

    TransferOrRequestDTO getTransferOrRequestDto(TransferRequest transferRequest) {
        TransferOrRequestDTO transferRequestDTO = new TransferOrRequestDTO();
        transferRequestDTO.setFromAccount(transferRequest.getRaisedFrom().getId());
        transferRequestDTO.setToAccount(transferRequest.getAcceptedBy().getId());
        transferRequestDTO.setAmount(transferRequest.getAmount());
        transferRequestDTO.setDescription(transferRequest.getTransferRequestStatus());
        transferRequestDTO.setRequestId(transferRequest.getRequestId());
        return transferRequestDTO;
    }
    public List<TransferOrRequestDTO> getTransferRequestsToUser(User currentUser) {
        List<Account> accounts = accountRepository.findByUserAndIsActive(currentUser, true);
        List<TransferOrRequestDTO> transferRequestList = new ArrayList<>();
        for(Account account:accounts) {
            transferRequestList.addAll(transferRequestRepository.findByAcceptedBy(account).stream()
                    .map(transferRequest
                            -> getTransferOrRequestDto(transferRequest))
                    .collect(Collectors.toList()));
        }
        return transferRequestList;
    }

    public List<TransferOrRequestDTO> getTransferRequestsFromUser(User currentUser) {
        List<Account> accounts = accountRepository.findByUserAndIsActive(currentUser, true);
        List<TransferOrRequestDTO> transferRequestList = new ArrayList<>();
        for(Account account:accounts) {
            transferRequestList.addAll(transferRequestRepository.findByRaisedFrom(account).stream()
                    .map(transferRequest
                            -> getTransferOrRequestDto(transferRequest))
                    .collect(Collectors.toList()));
        }
        return transferRequestList;
    }

    boolean validateForRequest(TransactionDTO transactionDTO) {
        boolean valid = true;
        Account fromAccount = accountRepository.findByIdAndIsActive(transactionDTO.getFromAccount(), true).orElse(null);
        Account toAccount = accountRepository.findByIdAndIsActive(transactionDTO.getToAccount(), true).orElse(null);
        valid &= (fromAccount!=null);
        valid &= (toAccount!=null);
        return valid;
    }

    @Transactional
    public void raiseTransferRequest(TransferOrRequestDTO transferOrRequestDTO) {
        TransactionDTO transactionDTO = null;
        if (transferOrRequestDTO.getMode().equals("account"))
            transactionDTO = getTransactionDto(transferOrRequestDTO);
        else {
            transactionDTO = userService.transferByEmailOrPhone(transferOrRequestDTO);
        }
        if(!validateForRequest(transactionDTO))
            throw  new GenericRuntimeException("Failed in request");

        Account fromAccount = accountRepository.findByIdAndIsActive(transactionDTO.getFromAccount(), true).orElse(null);
        Account toAccount = accountRepository.findByIdAndIsActive(transactionDTO.getToAccount(), true).orElse(null);
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setAmount(transactionDTO.getTransactionAmount());
        transferRequest.setTransferRequestStatus(StatusType.PENDING);
        transferRequest.setAcceptedBy(toAccount);
        transferRequest.setRaisedFrom(fromAccount);
        transferRequest.setCreatedTime(Instant.now());
        transferRequestRepository.save(transferRequest);
    }

    private TransactionDTO getTransactionDto(TransferOrRequestDTO transferOrRequestDTO) {
        TransactionDTO transactionDTO = new TransactionDTO();
        transactionDTO.setTransactionType(TransactionType.DEBIT);
        transactionDTO.setTransactionAmount(transferOrRequestDTO.getAmount());
        transactionDTO.setToAccount(transferOrRequestDTO.getFromAccount());
        transactionDTO.setFromAccount(transferOrRequestDTO.getToAccount());
        transactionDTO.setCreatedDate(Instant.now());
        return transactionDTO;
    }

    @Transactional
    public TransactionDTO transferByRequest(Long requestId) {
        TransferRequest transferRequest = transferRequestRepository.findByRequestId(requestId).orElse(null);
        if(transferRequest==null)
            throw new GenericRuntimeException("Invalid Request");
        TransactionDTO transactionDTO = getTransactionDto(transferRequest);
        transactionDTO.setTransactionType(TransactionType.DEBIT);
        Transaction transaction = transactionService.createTransaction(transactionDTO, StatusType.APPROVED);
        transferRequest.setUpdatedTime(Instant.now());
        transferRequest.setTransferRequestStatus(StatusType.APPROVED);
        transferRequestRepository.save(transferRequest);
        return transactionDTO;
    }

    private TransactionDTO getTransactionDto(TransferRequest transferRequest) {

        TransactionDTO transactionDTO = new TransactionDTO();
        transactionDTO.setFromAccount(transferRequest.getRaisedFrom().getId());
        transactionDTO.setToAccount(transferRequest.getAcceptedBy().getId());
        transactionDTO.setTransactionAmount(transferRequest.getAmount());
        transactionDTO.setDescription(transferRequest.getTransferRequestStatus());
        transactionDTO.setRequestId(transferRequest.getRequestId());
        return transactionDTO;
    }

    @Transactional
    public void denyTransferRequest(Long requestId) {
        TransferRequest transferRequest = transferRequestRepository.findByRequestId(requestId).orElse(null);
        if(transferRequest==null)
            throw new GenericRuntimeException("Invalid Request");
        transferRequest.setUpdatedTime(Instant.now());
        transferRequest.setTransferRequestStatus(StatusType.DECLINED);
        transferRequestRepository.save(transferRequest);
    }
    /*
    public void createAdditionalAccountRequest(CreateAccountDTO createAccountDTO, String requestType) {

        Request request = new Request();
        request.setCreatedDate(Instant.now());
        request.setDeleted(false);
        request.setDescription(requestType);
        request.setStatus(TransactionStatus.PENDING);
        request.setRequestType(requestType);
        request.setRequestBy(userService.getCurrentUser());

        String accNum = accountService.getNumericString(MAX_ACCOUNT_NUM_LEN);
        createAccountDTO.setAccountNumber(accNum);
        Account account = accountService.createAccount(userService.getCurrentUser(), createAccountDTO);
        request.setLinkedAccount();
        requestRepository.save(request);
    }
    */
}
