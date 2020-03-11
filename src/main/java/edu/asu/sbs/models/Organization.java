package edu.asu.sbs.models;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
public class Organization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long organizationId;

    @NotNull
    private String organizationName;

    @OneToOne
    @JoinColumn(nullable = false)
    private User representative;

}
