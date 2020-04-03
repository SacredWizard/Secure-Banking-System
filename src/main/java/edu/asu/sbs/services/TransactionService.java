package edu.asu.sbs.services;

import com.google.common.collect.Lists;
import edu.asu.sbs.config.RequestType;
import edu.asu.sbs.config.StatusType;
import edu.asu.sbs.config.TransactionStatus;
import edu.asu.sbs.config.TransactionType;
import edu.asu.sbs.errors.GenericRuntimeException;
import edu.asu.sbs.models.*;
import edu.asu.sbs.repositories.*;
import edu.asu.sbs.services.dto.ChequeDTO;
import edu.asu.sbs.services.dto.TransactionDTO;
import edu.asu.sbs.services.dto.TransferOrRequestDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static edu.asu.sbs.config.Constants.CRITICAL_TRANSACTION_LIMIT;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionAccountLogRepository transactionAccountLogRepository;
    private final ChequeRepository chequeRepository;
    private final RequestRepository requestRepository;
    private final UserService userService;
    private final TransactionHyperledgerService transactionHyperledgerService;
    private final TransferRequestRepository transferRequestRepository;

    public TransactionService(TransactionRepository transactionRepository, AccountRepository accountRepository, TransactionAccountLogRepository transactionAccountLogRepository, ChequeRepository chequeRepository, RequestRepository requestRepository, UserService userService, TransactionHyperledgerService transactionHyperledgerService, TransferRequestRepository transferRequestRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.transactionAccountLogRepository = transactionAccountLogRepository;
        this.chequeRepository = chequeRepository;
        this.requestRepository = requestRepository;
        this.userService = userService;
        this.transactionHyperledgerService = transactionHyperledgerService;
        this.transferRequestRepository = transferRequestRepository;
    }

    public List<TransactionDTO> getTransactions() {
        List<TransactionDTO> transactionDTOList = Lists.newArrayList();
        transactionRepository.findAll().forEach(transaction -> {
            TransactionDTO transactionDTO = new TransactionDTO();
            transactionDTO.setTransactionId(transaction.getTransactionId());
            transactionDTO.setStatus(transaction.getStatus());
            transactionDTO.setDescription(transaction.getDescription());
            transactionDTO.setTransactionType(transaction.getTransactionType());
            transactionDTO.setTransactionAmount(transaction.getTransactionAmount());
            transactionDTO.setFromAccount(transaction.getFromAccount().getId());
            transactionDTO.setToAccount(transaction.getToAccount().getId());
            transactionDTO.setModifiedDate(transaction.getModifiedTime());
            transactionDTO.setCreatedDate(transaction.getCreatedTime());
            if (transaction.getRequest() != null) {
                transactionDTO.setRequestId(transaction.getRequest().getRequestId());
            }
            transactionDTO.setTransactionAccountLog(transaction.getLog().getLogNumber());
            transactionDTOList.add(transactionDTO);
        });

        return transactionDTOList;
    }

    @Transactional
    public Transaction createTransaction(TransactionDTO transactionDTO, String transactionStatus) {
        if (validateTransaction(transactionDTO, transactionStatus)) {
            Optional<Account> optionalFromAccount = accountRepository.findById(transactionDTO.getFromAccount());
            Optional<Account> optionalToAccount = accountRepository.findById(transactionDTO.getToAccount());
            if (optionalFromAccount.isPresent() && optionalToAccount.isPresent() && transactionDTO.getTransactionAmount() > 0) {
                Account fromAccount = optionalFromAccount.get();
                Account toAccount = optionalToAccount.get();
                switch (transactionDTO.getTransactionType()) {
                    case TransactionType.DEBIT:
                        if (fromAccount.getAccountBalance() >= transactionDTO.getTransactionAmount()) {
                            fromAccount.setAccountBalance(fromAccount.getAccountBalance() - transactionDTO.getTransactionAmount());
                            if (transactionDTO.getTransactionAmount() <= CRITICAL_TRANSACTION_LIMIT && !transactionStatus.equals(TransactionStatus.PENDING)) {
                                toAccount.setAccountBalance(toAccount.getAccountBalance() + transactionDTO.getTransactionAmount());
                            }
                        } else {
                            throw new GenericRuntimeException("YOU ARE POOR, Account Balance not sufficient ¯\\_(ツ)_/¯");
                        }
                        break;
                    case TransactionType.CREDIT:
                        if (toAccount.getAccountBalance() >= transactionDTO.getTransactionAmount()) {
                            toAccount.setAccountBalance(toAccount.getAccountBalance() - transactionDTO.getTransactionAmount());
                            if (transactionDTO.getTransactionAmount() <= CRITICAL_TRANSACTION_LIMIT && !transactionStatus.equals(TransactionStatus.PENDING)) {
                                fromAccount.setAccountBalance(fromAccount.getAccountBalance() + transactionDTO.getTransactionAmount());
                            }
                        } else {
                            throw new GenericRuntimeException("No money huh, Account Balance not sufficient ¯\\_(ツ)_/¯");
                        }
                        break;
                    default:
                        throw new GenericRuntimeException("Do some valid steps bud, Invalid Transaction Type ¯\\_(ツ)_/¯");
                }
                TransactionAccountLog transactionAccountLog = new TransactionAccountLog();
                transactionAccountLog.setLogDescription(transactionDTO.getDescription());
                transactionAccountLog.setLogTime(Instant.now());
                transactionAccountLog.setAccount(fromAccount);
                transactionAccountLog = transactionAccountLogRepository.save(transactionAccountLog);
                Transaction transaction = new Transaction();
                transaction.setCreatedTime(Instant.now());
                transaction.setModifiedTime(Instant.now());
                transaction.setDescription(transactionDTO.getDescription());
                transaction.setFromAccount(fromAccount);
                if (transactionDTO.getTransactionAmount() > CRITICAL_TRANSACTION_LIMIT) {
                    transaction.setStatus(TransactionStatus.PENDING);
                } else {
                    transaction.setStatus(transactionStatus);
                }
                transaction.setTransactionType(transactionDTO.getTransactionType());
                transaction.setTransactionAmount(transactionDTO.getTransactionAmount());
                transaction.setLog(transactionAccountLog);
                transaction.setToAccount(toAccount);
                transaction.setFromAccount(fromAccount);
                accountRepository.save(toAccount);
                accountRepository.save(fromAccount);
                transaction = transactionRepository.save(transaction);
                if (transactionDTO.getTransactionAmount() > CRITICAL_TRANSACTION_LIMIT) {
                    Request request = new Request();
                    request.setCreatedDate(Instant.now());
                    request.setLinkedTransaction(transaction);
                    request.setDeleted(false);
                    request.setModifiedDate(Instant.now());
                    request.setDescription("Transaction more than 1000$");
                    request.setStatus(TransactionStatus.PENDING);
                    request.setRequestType(RequestType.APPROVE_CRITICAL_TRANSACTION);
                    request.setRequestBy(userService.getCurrentUser());
                    requestRepository.save(request);
                }
//                if (transaction.getStatus().equals(StatusType.APPROVED)) {
                transactionHyperledgerService.save(transaction);
//                }
                return transaction;
            }
        }

        return null;
    }

    private boolean validateTransaction(TransactionDTO transactionDTO, String transactionStatus) {
        if (!transactionDTO.getFromAccount().equals(transactionDTO.getToAccount())) {
            switch (transactionStatus) {
                case TransactionStatus.APPROVED:
                case TransactionStatus.PENDING:
                case TransactionStatus.REJECTED:
                    return true;
                default:
                    return false;
            }
        }
        return false;
    }

    @Transactional
    public void issueCheque(TransactionDTO transactionDTO) {
        transactionDTO.setTransactionType(TransactionType.DEBIT);
        Transaction transaction = createTransaction(transactionDTO, TransactionStatus.PENDING);
        Cheque cheque = new Cheque();
        cheque.setAmount(transactionDTO.getTransactionAmount());
        cheque.setTransaction(transaction);
        cheque.setChequeFromAccount(transaction.getFromAccount());
        cheque.setChequeToAccount(transaction.getToAccount());
        chequeRepository.save(cheque);
    }

    @Transactional
    public String clearCheque(Long chequeId) {
        Optional<Cheque> optionalCheque = chequeRepository.findById(chequeId);
        if (optionalCheque.isPresent()) {
            Cheque cheque = optionalCheque.get();
            Transaction transaction = cheque.getTransaction();
            if (transaction.getStatus().equals(TransactionStatus.PENDING) && !cheque.isDeleted()) {
                if (transaction.getTransactionAmount() > 1000) {
                    Request request = transaction.getRequest();
                    if (!request.getStatus().equals(TransactionStatus.APPROVED)) {
                        cheque.setDeleted(true);
                        chequeRepository.save(cheque);
                        return "Transaction to be Approved by TIER2";
                    }
                }
                Account toAccount = transaction.getToAccount();
                if (toAccount.isActive()) {
                    toAccount.setAccountBalance(toAccount.getAccountBalance() + transaction.getTransactionAmount());
                    transaction.setStatus(TransactionStatus.APPROVED);
                    TransactionAccountLog transactionAccountLog = transaction.getLog();
                    transactionAccountLog.setLogDescription("CHEQUE CLEARED");
                    cheque.setDeleted(true);
                    transactionRepository.save(transaction);
//                    if (transaction.getStatus().equals(StatusType.APPROVED)) {
                    transactionHyperledgerService.save(transaction);
//                    }
                    chequeRepository.save(cheque);
                    transactionAccountLogRepository.save(transactionAccountLog);
                    accountRepository.save(toAccount);
                    return "Cheque Cleared";
                }
            }
            throw new GenericRuntimeException("TIME TO DANCE, Error clearing cheque, contact admin ¯\\_(ツ)_/¯");
        } else {
            return "Cheque Not Found";
        }
    }

    @Transactional
    public void approveCriticalTransaction(Long requestId) {
        Optional<Request> optionalRequest = requestRepository.findOneByRequestId(requestId);
        optionalRequest.ifPresent(request -> {
            Transaction transaction = request.getLinkedTransaction();
            if (request.getStatus().equals(TransactionStatus.PENDING) && transaction.getTransactionAmount() > 1000) {
                request.setDescription("Critical Request Approved");
                request.setModifiedDate(Instant.now());
                request.setApprovedBy(userService.getCurrentUser());
                request.setStatus(TransactionStatus.APPROVED);
                requestRepository.save(request);
            }
        });
    }

    public List<ChequeDTO> getCheques() {
        List<ChequeDTO> chequeDTOList = Lists.newArrayList();
        chequeRepository.findAll().forEach(cheque -> {
            ChequeDTO chequeDTO = new ChequeDTO();
            chequeDTO.setChequeId(cheque.getChequeId());
            chequeDTO.setAccountId(cheque.getTransaction().getFromAccount().getId());
            chequeDTO.setUserId(cheque.getChequeFromAccount().getUser().getId());
            chequeDTO.setTransactionId(cheque.getTransaction().getTransactionId());
            chequeDTO.setTransactionStatus(cheque.getTransaction().getStatus());
            chequeDTO.setChequeAmount(cheque.getAmount());
            chequeDTOList.add(chequeDTO);
        });
        return chequeDTOList;
    }

    @Transactional
    public void denyCheque(Long chequeId) {
        Optional<Cheque> optionalCheque = chequeRepository.findById(chequeId);
        optionalCheque.ifPresent(cheque -> {
            Transaction transaction = cheque.getTransaction();
            if (transaction.getTransactionType().equals(TransactionStatus.PENDING) && !cheque.isDeleted()) {
                TransactionAccountLog transactionAccountLog = transaction.getLog();
                transactionAccountLog.setLogDescription("CHEQUE DECLINED");
                transactionAccountLog.setLogTime(Instant.now());
                Account fromAccount = cheque.getChequeFromAccount();
                fromAccount.setAccountBalance(cheque.getChequeFromAccount().getAccountBalance() + cheque.getAmount());
                cheque.setDeleted(true);
                transaction.setModifiedTime(Instant.now());
                transaction.setDescription("Transaction Declined");
                transaction.setStatus(StatusType.DECLINED);
                transactionRepository.save(transaction);
                chequeRepository.save(cheque);
                transactionAccountLogRepository.save(transactionAccountLog);
                accountRepository.save(fromAccount);
            }
        });
        throw new GenericRuntimeException("I just can't do this. Why don't you give up. Issue with denying cheque, contact admin ¯\\_(ツ)_/¯");
    }
}
