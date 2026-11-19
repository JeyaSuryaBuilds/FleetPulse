package com.fayzo.fleetpulse.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VehicleCategoryRepository extends JpaRepository<VehicleCategory, Long>, JpaSpecificationExecutor<VehicleCategory> {}
