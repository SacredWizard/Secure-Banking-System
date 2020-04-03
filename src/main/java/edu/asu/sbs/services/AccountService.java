package edu.asu.sbs.services;

import com.google.common.collect.Lists;
import edu.asu.sbs.config.UserType;
import edu.asu.sbs.config.RequestType;
import edu.asu.sbs.config.StatusType;
import edu.asu.sbs.errors.GenericRuntimeException;
import edu.asu.sbs.globals.AccountType;
import edu.asu.sbs.globals.CreditDebitType;
import edu.asu.sbs.models.Account;
import edu.asu.sbs.models.Request;
import edu.asu.sbs.models.User;
import edu.asu.sbs.repositories.AccountRepository;
import edu.asu.sbs.repositories.RequestRepository;
import edu.asu.sbs.repositories.TransactionAccountLogRepository;
import edu.asu.sbs.services.dto.NewAccountRequestDTO;
import edu.asu.sbs.services.dto.CreditDebitDTO;
import edu.asu.sbs.services.dto.ViewAccountDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static edu.asu.sbs.config.Constants.*;

@Slf4j
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionAccountLogRepository transactionAccountLogRepository;
    private RequestRepository accountRequestRepository;

    public AccountService(AccountRepository accountRepository, TransactionAccountLogRepository transactionAccountLogRepository, RequestRepository accountRequestRepository) {
        this.accountRepository = accountRepository;
        this.transactionAccountLogRepository = transactionAccountLogRepository;
        this.accountRequestRepository = accountRequestRepository;
    }

    public List<ViewAccountDTO> getAccounts() {
        List<ViewAccountDTO> viewAccountDTOList = Lists.newArrayList();
        accountRepository.findAll().forEach(account -> {
            ViewAccountDTO viewAccountDTO = new ViewAccountDTO();
            viewAccountDTO.setAccountId(account.getId());
            viewAccountDTO.setAccountType(account.getAccountType().name());
            viewAccountDTO.setBalance(account.getAccountBalance());
            viewAccountDTO.setUserId(account.getUser().getId());
            viewAccountDTOList.add(viewAccountDTO);
        });
        return viewAccountDTOList;
    }

    public Account getDefaultAccount(User user) {
        return accountRepository.findAccountByUserAndDefaultAccount(user, true).get();
    }

    public NewAccountRequestDTO createAccount(User customer, NewAccountRequestDTO newAccountRequestDTO) {
        Account newAccount = new Account();
        newAccount.setAccountBalance(newAccountRequestDTO.getInitialDeposit());
        newAccount.setAccountType(newAccountRequestDTO.getAccountType());
        newAccount.setUser(customer);
        if(newAccountRequestDTO.getAccountNumber() != null) {
            //If we allow user to set her desired account number, then we need to handle if DB save fails
            newAccount.setAccountNumber(newAccountRequestDTO.getAccountNumber());
        }
        accountRepository.save(newAccount);
        Request accountRequest = new Request();
        accountRequest.setRequestType(RequestType.CREATE_NEW_ACCOUNT);
        accountRequest.setCreatedDate(Instant.now());
        accountRequest.setDescription("New account request by user "+customer.getUserName());
        accountRequest.setLinkedAccount(newAccount);
        accountRequest.setRequestBy(customer);
        accountRequest.setStatus(StatusType.PENDING);
        accountRequestRepository.save(accountRequest);
        newAccountRequestDTO.setAccountNumber(newAccount.getAccountNumber());
        return newAccountRequestDTO;
    }

    public void credit(Account account, Double amount) throws Exception {
        try {
            Double currentBalance = account.getAccountBalance();
            account.setAccountBalance(currentBalance + amount);
            accountRepository.save(account);
        } catch (Exception e) {
            throw new Exception("Failed to credit from account " + account.getAccountNumber(), e);
        }
    }

    public List<Account> getAccountsForUser(User user) {
        return accountRepository.findByUserAndIsActive(user, true);
    }

    @Transactional
    public void makeSelfTransaction(User currentUser, CreditDebitDTO creditDebitRequest) throws Exception {
        List<Account> currentUserAccounts = accountRepository.findByUserAndLock(currentUser);
        for (Account currentUserAccount : currentUserAccounts) {
            if (currentUserAccount.getId().equals(creditDebitRequest.getId())) {
                System.out.println("Accounts :\n" + currentUserAccount.getId());
                if (creditDebitRequest.getCreditDebitType() == CreditDebitType.CREDIT) {
                    credit(currentUserAccount, creditDebitRequest.getAmount());
                } else if (creditDebitRequest.getCreditDebitType() == CreditDebitType.DEBIT) {
                    debit(currentUserAccount, creditDebitRequest.getAmount());
                }
                return;
            }
        }
        throw new Exception("Invalid Account");
    }

    private void debit(Account account, Double amount) throws Exception {
        try {
            Double currentBalance = account.getAccountBalance();
            if (currentBalance < amount)
                throw new Exception("Insufficient Funds");
            if (account.isActive()) {
                account.setAccountBalance(currentBalance - amount);
                accountRepository.save(account);
            } else {
                throw new Exception("Inactive account");
            }
        } catch (Exception e) {
            throw new Exception("Failed to debit from account " + account.getAccountNumber(), e);
        }
    }

    public Optional<Account> getAccountById(Long id) {
        return (accountRepository.getAccountById(id));
    }

    public void updateAccountType(Long accountId, AccountType accountType) {
        Optional<Account> account = getAccountById(accountId);
        account.ifPresent(account1 -> {
            account1.setAccountType(accountType);
            accountRepository.save(account1);
        });
    }

    public void closeUserAccount(Long id) {

        if (id != null) {
            Optional<Account> account = getAccountById(id);
            if (!account.get().isDefaultAccount()) {
                account.ifPresent(account1 -> {
                    account1.setActive(false);
                    accountRepository.save(account1);
                });
            } else {
                throw new GenericRuntimeException("Cannot close the default account");
            }
        }
    }

    public void deleteAccount(Account account) {
        accountRepository.delete(account);
    }

    public List<NewAccountRequestDTO> getPendingAccountsForUser(User currentUser) {
        List<Account> pendingAccounts = accountRepository.findByUserAndIsActive(currentUser, false);
        List<NewAccountRequestDTO> pendingAccountDTOList= new ArrayList<NewAccountRequestDTO>();
        for(Account pendingAccount:pendingAccounts) {
            NewAccountRequestDTO pendingAccountDTO = new NewAccountRequestDTO();
            pendingAccountDTO.setAccountNumber(pendingAccount.getAccountNumber());
            pendingAccountDTO.setAccountType(pendingAccount.getAccountType());
            pendingAccountDTO.setInitialDeposit(pendingAccount.getAccountBalance());
            pendingAccountDTOList.add(pendingAccountDTO);
        }
        return pendingAccountDTOList;
    }

    public List<String> getAccountNumbersForUser(User currentUser) {
        List<Long> accountNumbers = new ArrayList<>();
        List<Account> userAccounts = accountRepository.findByUserAndIsActive(currentUser, true);
        return userAccounts.stream()
                .map(account->account.getAccountNumber())
                .collect(Collectors.toList());
    }
}
