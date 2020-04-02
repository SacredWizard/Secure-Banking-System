package edu.asu.sbs.services.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import edu.asu.sbs.config.Constants;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.Instant;

@Data
public class ProfileRequestDTO {

    @Pattern(regexp = Constants.PHONE_NUMBER_REGEX)
    private String phoneNumber;

    @Email
    private String email;

    @NotNull
    private Long userId;

    @NotNull
    private Long requestId;

    @NotNull
    private String status;

    @NotNull
    private String description;

    @NotNull
    private boolean roleChange;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Instant createdDate;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Instant modifiedDate;
    /*
    @NotNull
    @Column(nullable = false)
    private String requestType;
    */

}
