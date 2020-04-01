package edu.asu.sbs.services.dto;

import edu.asu.sbs.globals.AccountType;
import lombok.Data;

@Data
public class CreateAccountDTO {
    private Double initialDeposit;
    private String accountNumber;
    private AccountType accountType;
}
