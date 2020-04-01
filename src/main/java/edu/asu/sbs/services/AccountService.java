package edu.asu.sbs.services;

import com.google.common.collect.Lists;
import edu.asu.sbs.config.UserType;
import edu.asu.sbs.errors.GenericRuntimeException;
import edu.asu.sbs.globals.AccountType;
import edu.asu.sbs.globals.CreditDebitType;
import edu.asu.sbs.models.Account;
import edu.asu.sbs.models.User;
import edu.asu.sbs.repositories.AccountRepository;
import edu.asu.sbs.repositories.TransactionAccountLogRepository;
import edu.asu.sbs.services.dto.CreateAccountDTO;
import edu.asu.sbs.services.dto.CreditDebitDTO;
import edu.asu.sbs.services.dto.ViewAccountDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static edu.asu.sbs.config.Constants.*;

@Slf4j
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionAccountLogRepository transactionAccountLogRepository;

    public AccountService(AccountRepository accountRepository, TransactionAccountLogRepository transactionAccountLogRepository) {
        this.accountRepository = accountRepository;
        this.transactionAccountLogRepository = transactionAccountLogRepository;
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

    public void createAccount(User customer, CreateAccountDTO createAccountDTO) {
        Account newAccount = new Account();
        newAccount.setAccountBalance(createAccountDTO.getInitialDeposit());
//        newAccount.setAccountNumber(createAccountDTO.getAccountNumber());
        newAccount.setAccountType(createAccountDTO.getAccountType());
        newAccount.setUser(customer);
        accountRepository.save(newAccount);
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

    // generate account number
    static String getNumericString(int n) {
        String AlphaNumericString = "0123456789";

        StringBuilder sb = new StringBuilder(n);

        for (int i = 0; i < n; i++) {
            int index
                    = (int) (AlphaNumericString.length()
                    * Math.random());

            sb.append(AlphaNumericString
                    .charAt(index));
        }
        return sb.toString();
    }

    public void createDefaultAccount(User newCustomer) {

        // Give a default "CHECKING" account to the user with $500 in his account.
        String accountNum = getNumericString(MAX_ACCOUNT_NUM_LEN);

        // check if account number is existing.
        AtomicBoolean accountCreation = new AtomicBoolean(true);
        System.out.println("---------Checking if Account number is present-------");
        accountRepository.findByAccountNumber(accountNum).ifPresent(account -> {
            System.out.println("---------Account number is present-------");
            accountCreation.set(false);
            try {
                throw new GeneralSecurityException("Account number already exists");
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            }
        });
        /* if account number is unique, just create account and return the user. */
        if (accountCreation.get() == true) {
        Account newAccount = new Account();
        newAccount.setAccountNumber(accountNum);
        newAccount.setAccountBalance(INITIAL_DEPOSIT_AMOUNT);
        if (newCustomer.getUserType() == UserType.MERCHANT_ROLE) {
            newAccount.setAccountType(AccountType.CURRENT);
        } else {
            newAccount.setAccountType(DEFAULT_ACCOUNT_TYPE);
        }
        newAccount.setActive(true);
        newAccount.setUser(newCustomer);
        newAccount.setDefaultAccount(true);
        accountRepository.save(newAccount);
        log.info(newAccount.toString());
        }
    }

}
