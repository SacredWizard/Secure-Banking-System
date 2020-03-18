package edu.asu.sbs.models;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Entity
public class Organization implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long organizationId;

    @NotNull
    private String organizationName;

    @OneToOne
    @JoinColumn(nullable = false)
    private User representative;

}
