package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Entity
@Table(name = "leave_requests")
public class LeaveRequest extends BaseEntity {
    @NotNull
    @Column(name = "employee_id")
    private Long employeeId;

    @NotBlank @Size(max = 30)
    @Column(name = "leave_type")
    private String leaveType;

    @NotNull
    @Column(name = "from_date")
    private LocalDate fromDate;

    @NotNull
    @Column(name = "to_date")
    private LocalDate toDate;

    @NotBlank @Size(max = 20)
    @Column(name = "status")
    private String status = "PENDING";

    @Size(max = 300)
    @Column(name = "reason")
    private String reason;

    
    @Column(name = "decided_by")
    private Long decidedBy;
}
