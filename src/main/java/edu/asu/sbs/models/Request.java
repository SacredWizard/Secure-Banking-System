package edu.asu.sbs.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@Entity
@ToString
public class Request implements Serializable {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long requestId;

    @NotNull
    @Column(nullable = false)
    private String requestType;

    @NotNull
    @Column(nullable = false)
    private String description;

    @NotNull
    @Column(nullable = false)
    private boolean isDeleted = false;

    @CreatedDate
    private Instant createdDate;

    private String status;

    @LastModifiedDate
    private Instant modifiedDate;

    @JsonBackReference
    @ManyToOne
    @JoinColumn
    private User requestBy;

    @JsonBackReference
    @OneToOne
    @JoinColumn
    private User approvedBy;

    @JsonBackReference
    @OneToOne
    @JoinColumn
    private Transaction linkedTransaction;

    @JsonBackReference
    @ManyToOne
    @JoinColumn
    private Account linkedAccount;

    @JsonBackReference
    @OneToOne
    @JoinColumn
    private ProfileRequest linkedProfileRequest;

    @JsonBackReference
    @OneToOne
    @JoinColumn
    private TransferRequest linkedTransferRequest;

}
