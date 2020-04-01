package edu.asu.sbs.services.dto;

import edu.asu.sbs.config.Constants;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;

@Data
public class RequestDTO {

    @Pattern(regexp = Constants.PHONE_NUMBER_REGEX)
    private String phoneNumber;

    @Email
    private String email;

}
