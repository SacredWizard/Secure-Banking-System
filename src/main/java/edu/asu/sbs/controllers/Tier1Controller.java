package edu.asu.sbs.controllers;


import edu.asu.sbs.config.TransactionStatus;
import edu.asu.sbs.config.UserType;
import edu.asu.sbs.models.Account;
import edu.asu.sbs.models.Transaction;
import edu.asu.sbs.services.AccountService;
import edu.asu.sbs.services.TransactionService;
import edu.asu.sbs.services.dto.RequestDTO;
import edu.asu.sbs.services.dto.TransactionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@PreAuthorize("hasAnyAuthority('" + UserType.EMPLOYEE_ROLE1 + "')")
@RestController
@RequestMapping("/api/v1/tier1")
public class Tier1Controller {

    private final AccountService accountService;
    private final TransactionService transactionService;

    public Tier1Controller(AccountService accountService, TransactionService transactionService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    @GetMapping("/accounts")
    @ResponseBody
    public List<Account> getAccounts() {
        return accountService.getAccounts();
    }

    @GetMapping("/transactions")
    @ResponseBody
    public List<Transaction> viewTransactions() {
        return transactionService.getTransactions();
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void createTransaction(@RequestBody TransactionDTO transactionDTO) {
        transactionService.createTransaction(transactionDTO, TransactionStatus.APPROVED);
    }

    @PostMapping("/issueCheck")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void issueCheque(@RequestBody TransactionDTO transactionDTO) {
        transactionService.issueCheque(transactionDTO);
    }

    @PutMapping("/clearCheck")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void clearCheck() {

    }

    @PostMapping("/raiseRequest")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void raiseRequest(@RequestBody RequestDTO requestDTO) {

    }

}
