package com.empay.parameterservice.repository;

import com.empay.parameterservice.entities.UsersEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsersRepository extends JpaRepository<UsersEntity, Integer> {
}
