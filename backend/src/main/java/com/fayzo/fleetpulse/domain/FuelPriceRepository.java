package com.fayzo.fleetpulse.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FuelPriceRepository extends JpaRepository<FuelPrice, Long>, JpaSpecificationExecutor<FuelPrice> {}
