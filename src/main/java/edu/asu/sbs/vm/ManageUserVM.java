package edu.asu.sbs.vm;

import edu.asu.sbs.config.Constants;
import edu.asu.sbs.services.dto.UserDTO;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Size;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class ManageUserVM extends UserDTO {

    @Size(min = Constants.PASSWORD_MIN_LENGTH, max = Constants.PASSWORD_MAX_LENGTH)
    private String password;

    public ManageUserVM() {
        /*
        * For Jackson
        */
    }
}
