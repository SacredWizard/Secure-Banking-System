package edu.asu.sbs.models;

import io.swagger.models.auth.In;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sessionKey;

    private Timestamp sessionStart;

    private Timestamp sessionEnd;

    private Integer sessionTimeout;

    private Integer otp;

    @OneToOne
    @JoinColumn(nullable = false)
    private User linkedUser;

}
