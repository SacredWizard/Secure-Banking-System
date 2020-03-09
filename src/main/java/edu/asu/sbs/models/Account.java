package edu.asu.sbs.models;


import edu.asu.sbs.config.Constants;
import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@Table(name = "BankAccount")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Pattern(regexp = Constants.ACCOUNT_NUMBER_REGEX)
    @Column(name = "accountNumber", unique = true, nullable = false, length = 17)
    @Size(min = 1, max = 17)
    private String accountNumber;

    @NotNull
    @Column(name = "accountType", nullable = false, length = 50)
    private String accountType;

    @NotNull
    @Column(name = "accountBalance", nullable = false)
    private Double accountBalance;

    @NotNull
    @Column(name = "isActive", nullable = false)
    private boolean isActive;

    @ManyToOne
    @JoinColumn(name="FkCustomerId", nullable=false)
    private User user;

    @OneToMany(mappedBy="fromAccount")
    private Set<Transaction> debitTransactions = new HashSet<Transaction>();

    @OneToMany(mappedBy="toAccount")
    private Set<Transaction> creditTransactions = new HashSet<Transaction>();
}