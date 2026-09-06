package com.hypeline.infrastructure.brand;

import com.hypeline.domain.brand.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandJpaRepository extends JpaRepository<Brand, Long> {
}
