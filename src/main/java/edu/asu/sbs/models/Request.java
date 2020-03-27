package edu.asu.sbs.models;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@Entity
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

    @ManyToOne
    @JoinColumn
    private User requestBy;

    @OneToOne
    @JoinColumn
    private User approvedBy;

    @OneToOne
    @JoinColumn
    private Transaction linkedTransaction;

}
