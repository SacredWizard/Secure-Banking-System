package edu.asu.sbs.services.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import edu.asu.sbs.globals.AccountType;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.Instant;

@Data
public class AccountTypeChangeDTO {
    @NotNull
    String accountNumber;

    @NotNull
    AccountType fromAccountType;

    @NotNull
    AccountType toAccountType;

    @NotNull
    String userName;

    @NotNull
    Long requestId;

    @NotNull
    String description;

    @NotNull
    String status;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Instant createdDate;

}
