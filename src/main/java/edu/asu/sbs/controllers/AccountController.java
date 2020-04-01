package edu.asu.sbs.controllers;

import edu.asu.sbs.config.UserType;
import edu.asu.sbs.services.AccountService;
import edu.asu.sbs.services.dto.ViewAccountDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PreAuthorize("hasAnyAuthority('" + UserType.EMPLOYEE_ROLE1 + "','" + UserType.EMPLOYEE_ROLE2 + "')")
    @GetMapping("/accounts")
    public List<ViewAccountDTO> getAccounts() {
        return accountService.getAccounts();
    }

}
