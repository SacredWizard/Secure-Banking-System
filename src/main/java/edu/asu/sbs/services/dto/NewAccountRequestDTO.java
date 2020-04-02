package edu.asu.sbs.services.dto;

import edu.asu.sbs.globals.AccountType;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class NewAccountRequestDTO {
    @NotNull
    private Double initialDeposit;
    private String accountNumber;
    @NotNull
    private AccountType accountType;
}
