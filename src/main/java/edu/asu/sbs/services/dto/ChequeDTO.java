package edu.asu.sbs.services.dto;

import lombok.Data;

import javax.annotation.ParametersAreNonnullByDefault;

@Data
@ParametersAreNonnullByDefault
public class ChequeDTO {
    private Long chequeId;
    private Long accountId;
    private Long userId;
    private Long transactionId;
    private String transactionStatus;
    private Double chequeAmount;
}
