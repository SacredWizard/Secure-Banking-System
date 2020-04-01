package edu.asu.sbs.services.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.Instant;

@Data
public class TransactionDTO {

    private Long transactionId;

    private String status;

    @NotNull
    private String description;

    @NotNull
    private String transactionType;

    @Min(1)
    @NotNull
    private Double transactionAmount;

    @NotNull
    private Long fromAccount;

    @NotNull
    private Long toAccount;

    private Long requestId;

    private Long transactionAccountLog;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Instant createdDate;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Instant modifiedDate;

}
