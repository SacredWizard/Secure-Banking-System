package edu.asu.sbs.models;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import edu.asu.sbs.config.Constants;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Getter
@Setter
@Entity
@ToString
public class ProfileRequest implements Serializable {
    boolean changeRoleRequest = false;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long requestId;

    @Pattern(regexp = Constants.PHONE_NUMBER_REGEX)
    @Column(unique = true)
    private String phoneNumber;

    @Email
    @Size(min = 5, max = 254)
    @Column(unique = true, length = 254)
    private String email;

    @JsonManagedReference
    @OneToOne(mappedBy = "linkedProfileRequest")
    private Request request;
}
