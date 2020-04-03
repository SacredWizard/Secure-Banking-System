package edu.asu.sbs.services;

import com.google.common.collect.Lists;
import edu.asu.sbs.config.RequestType;
import edu.asu.sbs.config.StatusType;
import edu.asu.sbs.config.TransactionStatus;
import edu.asu.sbs.config.TransactionType;
import edu.asu.sbs.errors.GenericRuntimeException;
import edu.asu.sbs.globals.AccountType;
import edu.asu.sbs.models.*;
import edu.asu.sbs.repositories.*;
import edu.asu.sbs.services.dto.AccountTypeChangeDTO;
import edu.asu.sbs.services.dto.DetailedNewAccountRequestDTO;
import edu.asu.sbs.services.dto.ProfileRequestDTO;
import edu.asu.sbs.services.dto.Tier2RequestsDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
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
    private final TransactionHyperledgerService transactionHyperledgerService;

    public RequestService(RequestRepository requestRepository, TransactionRepository transactionRepository, TransactionAccountLogRepository transactionAccountLogRepository, AccountService accountService, UserRepository userRepository, UserService userService, ProfileRequestRepository profileRequestRepository, AccountRepository accountRepository, TransactionHyperledgerService transactionHyperledgerService) {
        this.requestRepository = requestRepository;
        this.transactionRepository = transactionRepository;
        this.transactionAccountLogRepository = transactionAccountLogRepository;
        this.accountService = accountService;
        this.userRepository = userRepository;
        this.userService = userService;
        this.profileRequestRepository = profileRequestRepository;
        this.accountRepository = accountRepository;
        this.transactionHyperledgerService = transactionHyperledgerService;
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
            //saRequestDTO.setModifiedDate(request.getModifiedDate());
            RequestDTO.setUserId(request.getRequestBy().getId());
            RequestDTOList.add(RequestDTO);
        }
        return RequestDTOList;
    }

    public List<Request> getEmpProfileUpdateRequests() {
        List<Request> requestList = Lists.newArrayList();
        requestList.addAll(requestRepository.findByRequestTypeInAndIsDeleted(Lists.newArrayList(RequestType.UPDATE_EMP_PROFILE), false));
        return requestList;
    }

    public List<Request> getUserProfileUpdateRequests() {
        List<Request> requestList = Lists.newArrayList();
        requestList.addAll(requestRepository.findByRequestTypeInAndIsDeleted(Lists.newArrayList(RequestType.UPDATE_USER_PROFILE), false));
        return requestList;
    }

    public Optional<Request> getRequest(Long RequestId) {
        return requestRepository.findOneByRequestId(RequestId);
    }

    public List<Tier2RequestsDTO> getAllTransactionRequests() {
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
        log.info(Instant.now() + ": Transaction approved/declined. ID: " + transaction.getTransactionId());
        transactionAccountLogRepository.save(transactionAccountLog);
        transactionRepository.save(transaction);
//        if (transaction.getStatus().equals(StatusType.APPROVED)) {
            transactionHyperledgerService.save(transaction);
//        }
        requestRepository.save(request);
    }

    @Transactional
    public void updateUserProfile(Request request, User approver, String requestType, String action) {

        if (request.getLinkedProfileRequest().getPhoneNumber().isEmpty() && request.getLinkedProfileRequest().getEmail().isEmpty()) {
            log.error(Instant.now() + ": Both phone number and email are empty for the request. Request ID:" + request.getRequestId());
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
                log.info(Instant.now() + ": Profile Updated for the user. UserID: " + user.getId());
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
        if (StatusType.DECLINED.equals(action))
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
        log.info(Instant.now() + ": Request approved/declined. RequestID: " + request.getRequestId());
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

        log.info(Instant.now() + ": Request created for profile update. RequestID: " + request.getRequestId());
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
        log.info(Instant.now() + ": Request created for Change Role. RequestID: " + request.getRequestId());
    }

    @Transactional
    public void updateChangeRoleRequest(Request request, String status, User approver) {
        request.setStatus(status);
        request.setApprovedBy(approver);
        request.setModifiedDate(Instant.now());
        request.setDeleted(true);
        requestRepository.save(request);
        log.info(Instant.now() + ": Request aproved/declined for Change Role. RequestID: " + request.getRequestId());
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

    @Transactional
    public void createAccountTypeChangeRequest(Account account, AccountType toAccount, User requester) {
        Request request = new Request();
        request.setCreatedDate(Instant.now());
        request.setDeleted(false);
        request.setDescription(RequestType.MODIFY_ACCOUNT_TYPE + " to " + toAccount);
        request.setStatus(TransactionStatus.PENDING);
        request.setRequestType(RequestType.MODIFY_ACCOUNT_TYPE);
        request.setRequestBy(requester);
        request.setLinkedAccount(account);
        requestRepository.save(request);
        log.info(Instant.now() + ": Request created for Account Type change. RequestID: " + request.getRequestId());
    }

    public List<AccountTypeChangeDTO> getAllAccountTypeChangeRequests() {
        List<AccountTypeChangeDTO> RequestDTOList = Lists.newArrayList();
        List<Request> requestList = requestRepository.findByRequestTypeInAndIsDeleted(Lists.newArrayList(RequestType.MODIFY_ACCOUNT_TYPE), false);
        for (Request request : requestList) {
            AccountTypeChangeDTO RequestDTO = new AccountTypeChangeDTO();
            RequestDTO.setRequestId(request.getRequestId());
            RequestDTO.setStatus(request.getStatus());
            RequestDTO.setDescription(request.getDescription());
            RequestDTO.setCreatedDate(request.getCreatedDate());
            RequestDTO.setUserName(request.getRequestBy().getUserName());

            String[] toAccountType = request.getDescription().trim().split(" ");
            RequestDTO.setToAccountType(AccountType.valueOf(toAccountType[toAccountType.length - 1]));

            RequestDTO.setFromAccountType(request.getLinkedAccount().getAccountType());
            RequestDTO.setAccountNumber(request.getLinkedAccount().getAccountNumber());
            RequestDTOList.add(RequestDTO);
        }
        return RequestDTOList;
    }

    @Transactional
    public void updateAccountType(Long requestId, String action, User approver) {
        Optional<Request> request = getRequest(requestId);
        request.ifPresent(req -> {
            String[] splitter = req.getDescription().trim().split(" ");
            AccountType accountType = AccountType.valueOf(splitter[splitter.length - 1]);
            req.setApprovedBy(approver);
            req.setModifiedDate(Instant.now());
            Account account = req.getLinkedAccount();

            if (action.equals(StatusType.APPROVED)) {
                req.setStatus(StatusType.APPROVED);
                account.setAccountType(accountType);
            } else if (action.equals(StatusType.DECLINED)) {
                req.setStatus(StatusType.DECLINED);
            } else {
                throw new GenericRuntimeException("Invalid Action provided: " + action);
            }
            req.setDeleted(true);
            requestRepository.save(req);
            accountRepository.save(account);
            log.info(Instant.now() + ": Request updated for AccountType change. RequestID: " + req.getRequestId());
        });
    }

}
