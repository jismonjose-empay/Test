package com.empay.parameterservice.repository;

import com.empay.parameterservice.entities.CountriesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CountriesRepository extends JpaRepository<CountriesEntity, String>,
        JpaSpecificationExecutor<CountriesEntity> {

    boolean existsByCountryName(String countryName);

    boolean existsByAlpha2Code(String alpha2Code);

    boolean existsByAlpha3Code(String alpha3Code);

}
