package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Entity
@Table(name = "employees")
public class Employee extends BaseEntity {
    @NotBlank @Size(max = 30)
    @Column(name = "employee_code")
    private String employeeCode = com.fayzo.fleetpulse.common.IdGen.code("EMP");

    @NotBlank @Size(max = 160)
    @Column(name = "full_name")
    private String fullName;

    @Size(max = 190)
    @Column(name = "email")
    private String email;

    @Size(max = 24)
    @Column(name = "mobile")
    private String mobile;

    
    @Column(name = "user_id")
    private Long userId;

    
    @Column(name = "driver_id")
    private Long driverId;

    
    @Column(name = "department_id")
    private Long departmentId;

    
    @Column(name = "branch_id")
    private Long branchId;

    
    @Column(name = "shift_id")
    private Long shiftId;

    @Size(max = 120)
    @Column(name = "job_title")
    private String jobTitle;

    @NotBlank @Size(max = 20)
    @Column(name = "status")
    private String status = "ACTIVE";

    
    @Column(name = "joined_on")
    private LocalDate joinedOn;
}
