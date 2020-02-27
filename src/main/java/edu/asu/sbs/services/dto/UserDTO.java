package edu.asu.sbs.services.dto;

import edu.asu.sbs.config.Constants;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.sql.Date;

@Data
public class UserDTO {
    private Long id;

    @NotBlank
    @Pattern(regexp = Constants.USERNAME_REGEX)
    @Size(min = 1, max = 50)
    private String userName;

    @Size(min = 1, max = 50)
    private String firstName;

    @Size(min = 1, max = 50)
    private String lastName;

    @DateTimeFormat(pattern = "mm-dd-yyyy")
    private Date dateOfBirth;

    @Email
    @Size(min = 5, max = 254)
    private String email;

    private boolean activated = false;

}
