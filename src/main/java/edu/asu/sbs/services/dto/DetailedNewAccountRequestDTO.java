package edu.asu.sbs.services.dto;

import edu.asu.sbs.globals.AccountType;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class DetailedNewAccountRequestDTO {
    @NotNull
    String accountNumber;
    @NotNull
    AccountType accountType;
    @NotNull
    Double initialDeposit;
    @NotNull
    String userName;
    @NotNull
    Long requestId;
}
