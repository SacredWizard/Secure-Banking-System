package edu.asu.sbs.services;

import com.google.common.collect.Lists;
import edu.asu.sbs.config.RequestType;
import edu.asu.sbs.config.StatusType;
import edu.asu.sbs.errors.GenericRuntimeException;
import edu.asu.sbs.globals.CreditDebitType;
import edu.asu.sbs.models.Account;
import edu.asu.sbs.models.Request;
import edu.asu.sbs.models.User;
import edu.asu.sbs.repositories.AccountRepository;
import edu.asu.sbs.repositories.RequestRepository;
import edu.asu.sbs.services.dto.CreditDebitDTO;
import edu.asu.sbs.services.dto.NewAccountRequestDTO;
import edu.asu.sbs.services.dto.ViewAccountDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final RequestRepository accountRequestRepository;

    public AccountService(AccountRepository accountRepository, RequestRepository accountRequestRepository) {
        this.accountRepository = accountRepository;
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
        log.info(Instant.now() + ": Adding a new account for the user: " + customer.getUserName());
        accountRepository.save(newAccount);
        Request accountRequest = new Request();
        accountRequest.setRequestType(RequestType.CREATE_NEW_ACCOUNT);
        accountRequest.setCreatedDate(Instant.now());
        accountRequest.setDescription("New account request by user "+customer.getUserName());
        accountRequest.setLinkedAccount(newAccount);
        accountRequest.setRequestBy(customer);
        accountRequest.setStatus(StatusType.PENDING);
        log.info(Instant.now() + ": Creating a new account request for the user: " + customer.getUserName());
        accountRequestRepository.save(accountRequest);
        newAccountRequestDTO.setAccountNumber(newAccount.getAccountNumber());
        return newAccountRequestDTO;
    }

    public void credit(Account account, Double amount) throws Exception {
        try {
            Double currentBalance = account.getAccountBalance();
            account.setAccountBalance(currentBalance + amount);
            log.info(Instant.now() + ": Credited:" + amount + " amount to the account: " + account.getAccountNumber());
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
                log.info(Instant.now() + ": Debited:" + amount + " amount from the account: " + account.getAccountNumber());
                accountRepository.save(account);
            } else {
                throw new Exception("Inactive account");
            }
        } catch (Exception e) {
            log.error(Instant.now() + "Failed to debit from account " + account.getAccountNumber());
            throw new Exception("Failed to debit from account " + account.getAccountNumber(), e);
        }
    }

    public Optional<Account> getAccountById(Long id) {
        return (accountRepository.getAccountById(id));
    }

    public void closeUserAccount(Long id) {

        if (id != null) {
            Optional<Account> account = getAccountById(id);
            if (!account.get().isDefaultAccount()) {
                account.ifPresent(account1 -> {
                    account1.setActive(false);
                    log.info(Instant.now() + ": Closed the account: " + account1.getAccountNumber());
                    accountRepository.save(account1);
                });
            } else {
                log.warn(Instant.now() + ": Cannot close the default account");
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

}
