package com.hypeline.domain.brand;

import java.util.Optional;

public interface BrandRepository {
    Brand save(Brand brand);

    Optional<Brand> findById(Long id);
}
