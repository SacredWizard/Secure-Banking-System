package edu.asu.sbs.models;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import edu.asu.sbs.globals.AccountType;
import edu.asu.sbs.globals.AccountTypeAttributeConverter;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
//@EqualsAndHashCode(exclude = {"accountLogs", "debitTransactions", "creditTransactions", "fromCheques", "toCheques"})
public class Account implements Serializable {

    private static final long serialVersionUID = -1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    @Convert(converter = AccountTypeAttributeConverter.class)
    private AccountType accountType;

    @NotNull
    @Column(nullable = false)
    private Double accountBalance;

    @NotNull
    @Column(nullable = false)
    private boolean isActive;

    @ManyToOne
    @JsonBackReference
    @JoinColumn(nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @NotNull
    @Column(nullable = false)
    private boolean defaultAccount;

    @JsonManagedReference
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "account")
    private Set<TransactionAccountLog> accountLogs = new HashSet<>();

    @JsonManagedReference
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "fromAccount")
    private Set<Transaction> debitTransactions = new HashSet<>();

    @JsonManagedReference
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "toAccount")
    private Set<Transaction> creditTransactions = new HashSet<>();

    @JsonManagedReference
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "chequeFromAccount")
    private Set<Cheque> fromCheques = new HashSet<>();

    @JsonManagedReference
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "chequeToAccount")
    private Set<Cheque> toCheques = new HashSet<>();

    @JsonManagedReference
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "linkedAccount")
    private Set<Request> request = new HashSet<>();

    @JsonManagedReference
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "acceptedBy")
    private Set<TransferRequest> incomingTransferRequests = new HashSet<>();

    @JsonManagedReference
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "raisedFrom")
    private Set<TransferRequest> outgoingTransferRequests = new HashSet<>();

    public void setAccountNumber(String accountNumberString) {
        id = Long.parseLong(accountNumberString);
    }

    public String getAccountNumber() {
        return id.toString();
    }

}
