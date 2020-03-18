package edu.asu.sbs.services;

import com.google.common.collect.Lists;
import edu.asu.sbs.models.Account;
import edu.asu.sbs.repositories.AccountRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public List<Account> getAccounts() {
        List<Account> accountList = Lists.newArrayList();
        accountRepository.findAll().forEach(accountList::add);
        return accountList;
    }


}
