package edu.asu.sbs.services.dto;

import lombok.Data;

import javax.annotation.ParametersAreNonnullByDefault;

@Data
@ParametersAreNonnullByDefault
public class ViewAccountDTO {

    private Long accountId;
    private String accountType;
    private Double balance;
    private Long userId;

}
