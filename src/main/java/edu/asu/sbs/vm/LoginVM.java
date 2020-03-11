package edu.asu.sbs.vm;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
public class LoginVM {

    @NotNull
    @Size(min = 1, max = 50)
    private String userName;

    @NotNull
    @Size(min = 4, max = 100)
    private String password;

    @NotNull
    private Boolean rememberMe;

}

