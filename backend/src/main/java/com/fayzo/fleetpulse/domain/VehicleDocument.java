package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Entity
@Table(name = "vehicle_documents")
public class VehicleDocument extends BaseEntity {
    @NotNull
    @Column(name = "vehicle_id")
    private Long vehicleId;

    @NotBlank @Size(max = 40)
    @Column(name = "doc_type")
    private String docType;

    @Size(max = 80)
    @Column(name = "doc_number")
    private String docNumber;

    @Size(max = 120)
    @Column(name = "provider")
    private String provider;

    
    @Column(name = "issue_date")
    private LocalDate issueDate;

    
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Size(max = 500)
    @Column(name = "file_url")
    private String fileUrl;
}
