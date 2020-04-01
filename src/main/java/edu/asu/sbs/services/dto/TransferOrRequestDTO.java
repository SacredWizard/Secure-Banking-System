package edu.asu.sbs.services.dto;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.Instant;

@Data
public class TransferOrRequestDTO {

    @NotNull
    private String mode;

    @NotNull
    private String description;

    @Min(1)
    @NotNull
    private Double amount;

    private Long toAccount;

    @Email
    private String email;

    private String phoneNumber;

    @NotNull
    private String type;
}
