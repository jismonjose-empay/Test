package com.empay.parameterservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.empay.parameterservice.entities.UsersEntityV2;

public interface UsersRepositoryV2 extends JpaRepository<UsersEntityV2,Integer>,JpaSpecificationExecutor<UsersEntityV2> {
	
	UsersEntityV2 findByUserId(String userId);
	
	UsersEntityV2 findByUserIdAndStatus(String userId,String status);
	
	void deleteByUserId(String userId);

	boolean existsBySerialNumberAndStatus(Integer serialNumber, String status);

    UsersEntityV2 findByUserIdAndStatusIn(String userId, String[] status);
    
    UsersEntityV2 findByEmail(String email);
    
    UsersEntityV2 findBySerialNumber(Integer serialNumber);
}
