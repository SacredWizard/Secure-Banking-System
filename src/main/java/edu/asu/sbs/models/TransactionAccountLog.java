package edu.asu.sbs.models;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Entity
public class TransactionAccountLog implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logNumber;

    private String logDescription;

    private Timestamp logTime;

    @OneToOne(mappedBy = "log")
    private Transaction transaction;

    @OneToOne(mappedBy = "log")
    private Account account;
}
