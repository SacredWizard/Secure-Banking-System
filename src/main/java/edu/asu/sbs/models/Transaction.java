package edu.asu.sbs.models;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Timestamp;

@Data
@Entity
public class Transaction implements Serializable {
    private static final long serialVersionUID = -1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @NotNull
    @Column(nullable = false, length = 50)
    private String status;

    @NotNull
    private String description;

    @NotNull
    private String transactionType;
    private Timestamp createdTime;
    private Double transactionAmount;
    private Timestamp updatedTime;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Account fromAccount;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Account toAccount;

}
