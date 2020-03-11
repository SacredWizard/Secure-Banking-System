package edu.asu.sbs.models;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long requestId;

    private Timestamp requestTime;

    private String requestType;

    private String description;

    @OneToOne
    @JoinColumn(nullable = true)
    private User requestBy;

    @OneToOne
    @JoinColumn(nullable = true)
    private Transaction linkedTransaction;

}
