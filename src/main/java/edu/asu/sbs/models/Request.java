package edu.asu.sbs.models;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;

@Data
@Entity
public class Request implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long requestId;

    private Timestamp requestTime;

    private String requestType;

    private String description;

    @NotNull
    @Column(nullable = false)
    private boolean isDeleted;

    @CreatedDate
    private Instant createdDate;

    private String status;

    @LastModifiedDate
    private Instant modifiedDate;

    @OneToOne
    @JoinColumn(nullable = true)
    private User requestBy;

    @OneToOne
    @JoinColumn(nullable = true)
    private User approvedBy;

    @OneToOne
    @JoinColumn(nullable = true)
    private Transaction linkedTransaction;

}
