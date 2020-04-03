package edu.asu.sbs.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.Instant;

@Getter
@Setter
@Entity
public class TransferRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long requestId;

    private Instant createdTime;

    private Instant updatedTime;

    private Double amount;

    @NotNull
    @Column(nullable = false)
    private String transferRequestStatus;

    @JsonBackReference
    @ManyToOne
    @JoinColumn
    private Account raisedFrom;

    @JsonBackReference
    @ManyToOne
    @JoinColumn
    private Account acceptedBy;


}
