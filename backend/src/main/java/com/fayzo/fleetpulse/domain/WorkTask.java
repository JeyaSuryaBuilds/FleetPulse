package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Entity
@Table(name = "tasks")
public class WorkTask extends BaseEntity {
    @NotBlank @Size(max = 160)
    @Column(name = "title")
    private String title;

    
    @Column(name = "description")
    private String description;

    
    @Column(name = "assigned_employee_id")
    private Long assignedEmployeeId;

    
    @Column(name = "due_date")
    private LocalDate dueDate;

    @NotBlank @Size(max = 20)
    @Column(name = "status")
    private String status = "OPEN";

    @Size(max = 10)
    @Column(name = "priority")
    private String priority;
}
