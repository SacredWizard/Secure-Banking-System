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
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Pattern(regexp = Constants.ACCOUNT_NUMBER_REGEX)
    @Column(unique = true, nullable = false, length = 17)
    @Size(min = 1, max = 17)
    private String accountNumber;

    @NotNull
    @Column(nullable = false, length = 50)
    private String accountType;

    @NotNull
    @Column(nullable = false)
    private Double accountBalance;

    @NotNull
    @Column(nullable = false)
    private boolean isActive;

    @ManyToOne
    @JoinColumn(nullable = false)
    private User user;

    @OneToOne
    @JoinColumn(nullable = false)
    private TransactionAccountLog log;

    @OneToMany(mappedBy = "fromAccount")
    private Set<Transaction> debitTransactions = new HashSet<>();

    @OneToMany(mappedBy = "toAccount")
    private Set<Transaction> creditTransactions = new HashSet<>();
}
