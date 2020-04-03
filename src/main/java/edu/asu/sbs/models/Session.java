package edu.asu.sbs.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@Entity
public class Session implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sessionKey;

    private Instant sessionStart;

    private Instant sessionEnd;

    private Integer sessionTimeout;

    private Integer otp;

    @JsonBackReference
    @OneToOne
    @JoinColumn(nullable = false)
    private User linkedUser;

}
