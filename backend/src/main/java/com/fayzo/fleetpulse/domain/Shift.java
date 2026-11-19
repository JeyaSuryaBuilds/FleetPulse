package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "shifts")
public class Shift extends BaseEntity {
    @NotBlank @Size(max = 80)
    @Column(name = "name")
    private String name;

    @NotBlank @Size(max = 8)
    @Column(name = "start_time")
    private String startTime;

    @NotBlank @Size(max = 8)
    @Column(name = "end_time")
    private String endTime;
}
