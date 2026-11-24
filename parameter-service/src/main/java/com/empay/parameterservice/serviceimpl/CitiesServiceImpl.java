package com.empay.parameterservice.serviceimpl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.empay.parameterservice.entities.CitiesEntity;
import com.empay.parameterservice.entities.StatesEntity;
import com.empay.parameterservice.exceptions.ResourceNotFoundException;
import com.empay.parameterservice.repository.CitiesRepo;
import com.empay.parameterservice.repository.StatesRepo;
import com.empay.parameterservice.service.CitiesService;
import com.empay.parameterservice.utils.APIConstants;
import com.empay.parameterservice.utils.JwtUtils;
import com.empay.parameterservice.valueobjects.CitiesVO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class CitiesServiceImpl implements CitiesService {

	private final CitiesRepo citiesRepo;
	private final JwtUtils jwtUtils;
	private final StatesRepo statesRepo;

	private static final DateTimeFormatter DateFormatTime = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

	private static final String CITY_NOT_FOUND = "City not found";

	@Override
	public ResponseEntity<Object> addCity(@Valid CitiesVO citiesVO, HttpServletRequest request) {
		try {
			if (Objects.isNull(citiesVO)) {
				return new ResponseEntity<>("Invalid Data", HttpStatus.EXPECTATION_FAILED);
			}
			citiesVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
			citiesRepo.saveAndFlush(mapCityVoToEntity(citiesVO));
			return new ResponseEntity<>("City Added Successfully", HttpStatus.OK);
		} catch (DataIntegrityViolationException e) {

			log.error("ERROR : addCity() :", e);
			String message = e.getRootCause() != null ? e.getRootCause().getMessage() : e.getMessage();
			if (message.contains("UQ_CTY_NAME_STE_CODE")) {
				return new ResponseEntity<>("This city already exists for the selected state.", HttpStatus.CONFLICT);
			}
			if (message.contains("FK_CTY_STE_CODE")) {
				return new ResponseEntity<>("Invalid state selected.", HttpStatus.CONFLICT);
			}
			return new ResponseEntity<>("Duplicate/Invalid data", HttpStatus.CONFLICT);
		} catch (Exception e) {
			log.error("ERROR : addCity() :", e);
			return new ResponseEntity<>(APIConstants.COMMON_FAILED_EXCEPTION_RESPONSE,
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private CitiesEntity mapCityVoToEntity(@Valid CitiesVO vo) {

	    CitiesEntity entity = new CitiesEntity();

	    entity.setSerialNumber(vo.getSerialNumber());
	    entity.setUpdatedUser(vo.getUpdatedUser());
	    entity.setLastUpdated(LocalDateTime.now(ZoneId.systemDefault()));
	    entity.setCityName(Optional.ofNullable(vo.getCityName()).orElse(""));
	    StatesEntity states = statesRepo.findByStateCode(vo.getStateCode());

	    if (states == null) {
	        throw new ResourceNotFoundException(
	            "State",                         
	            "stateCode",                     
	            "State not found with code: " + vo.getStateCode() 
	        );
	    }

	    entity.setState(states);
	    return entity;
	}

	@Override
	@Transactional
	public ResponseEntity<Object> updateCity(Integer id, @Valid CitiesVO citiesVO, HttpServletRequest request) {

		try {
			if (Objects.isNull(citiesVO)) {
				return new ResponseEntity<>(CITY_NOT_FOUND, HttpStatus.BAD_REQUEST);
			}

			if (citiesVO.getSerialNumber() != null && !id.equals(citiesVO.getSerialNumber())) {
			    throw new IllegalArgumentException("Invalid request. Please verify the submitted details.");
			}

			CitiesEntity existingEntity = citiesRepo.findById(id).orElseThrow(
					() -> new ResourceNotFoundException("City", "Serial Number", "City with ID " + id + " not found"));
			citiesVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
			CitiesEntity mappedEntity = mapCityVoToEntity(citiesVO);
			existingEntity.setCityName(mappedEntity.getCityName());
			existingEntity.setState(mappedEntity.getState());
			existingEntity.setLastUpdated(LocalDateTime.now(ZoneId.systemDefault()));
			existingEntity.setUpdatedUser(citiesVO.getUpdatedUser());

			citiesRepo.saveAndFlush(existingEntity);
			return new ResponseEntity<>("City Updated Successfully", HttpStatus.OK);

		} catch (DataIntegrityViolationException e) {
			log.error("ERROR : updateCity() :", e);
			String message = e.getRootCause() != null ? e.getRootCause().getMessage() : e.getMessage();
			if (message != null && message.contains("UQ_CTY_NAME_STE_CODE")) {
				return new ResponseEntity<>("This city already exists for the selected state.", HttpStatus.CONFLICT);
			}
			if (message != null && message.contains("FK_CTY_STE_CODE")) {
				return new ResponseEntity<>("Invalid state selected.", HttpStatus.CONFLICT);
			}
			return new ResponseEntity<>("Duplicate/Invalid data", HttpStatus.CONFLICT);
		} catch (ResourceNotFoundException e) {
			throw e;
		} catch (Exception e) {
			log.error("ERROR : updateCity() :", e);
			return new ResponseEntity<>(APIConstants.COMMON_FAILED_EXCEPTION_RESPONSE,
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ResponseEntity<Object> getCity(Integer id) {
		var entity = citiesRepo.findBySerialNumber(id);
		if (Objects.nonNull(entity)) {
			CitiesVO valueObject = mapCitiesEntityToVo(entity);
			return new ResponseEntity<>(valueObject, HttpStatus.OK);
		} else {
			return new ResponseEntity<>(CITY_NOT_FOUND, HttpStatus.EXPECTATION_FAILED);
		}
	}

	private CitiesVO mapCitiesEntityToVo(CitiesEntity entity) {
		CitiesVO vo = new CitiesVO();
		vo.setSerialNumber(entity.getSerialNumber());
		vo.setUpdatedUser(entity.getUpdatedUser());
		vo.setLastUpdated(Optional.ofNullable(DateFormatTime.format(entity.getLastUpdated())).orElse(""));
		vo.setCityName(Optional.ofNullable(entity).map(CitiesEntity::getCityName).orElse(""));
		vo.setStateName(
				Optional.ofNullable(entity).map(CitiesEntity::getState).map(StatesEntity::getStateName).orElse(""));
		vo.setStateCode(
				Optional.ofNullable(entity).map(CitiesEntity::getState).map(StatesEntity::getStateCode).orElse(""));
		return vo;
	}

	@Override
	@Transactional
	public ResponseEntity<Object> deleteCity(Integer id, HttpServletRequest request) {
		try {
			var entity = citiesRepo.findBySerialNumber(id);
			if (Objects.nonNull(entity)) {
				citiesRepo.delete(entity);
				citiesRepo.flush();
				return new ResponseEntity<>("City Deleted Successfully", HttpStatus.OK);
			} else {
				return new ResponseEntity<>(CITY_NOT_FOUND, HttpStatus.EXPECTATION_FAILED);
			}
		} catch (DataIntegrityViolationException e) {
			log.error("ERROR : deleteCity() :", e);
			return new ResponseEntity<>("Cannot delete City. It is already in use.", HttpStatus.CONFLICT);

		} catch (Exception e) {
			log.error("ERROR : deleteCity() :", e);
			return new ResponseEntity<>(APIConstants.COMMON_FAILED_EXCEPTION_RESPONSE,
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public Object getCities(Integer page, int size, String sortBy, String sortDir, String search) {
		if ("stateName".equalsIgnoreCase(sortBy)) {
			sortBy = "state.stateName";
		}
		Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

		Integer defaultPageNumber = Objects.nonNull(page) ? page - 1 : 0;
		Pageable pageable = PageRequest.of(defaultPageNumber, size, sort);
		Page<CitiesEntity> citiesPage;
		if (search != null && !search.trim().isEmpty()) {
			Specification<CitiesEntity> specification = (root, query, cb) -> cb.or(
					cb.like(cb.lower(root.get("cityName")), "%" + search.toLowerCase() + "%"),
					cb.like(cb.lower(root.join("state").get("stateName")), "%" + search.toLowerCase() + "%"));
			citiesPage = citiesRepo.findAll(specification, pageable);
		} else {
			citiesPage = citiesRepo.findAll(pageable);
		}
		List<CitiesVO> voList = citiesPage.getContent().stream().map(this::mapCitiesEntityToVo).toList();
		Map<String, Object> responseVO = new HashMap<>();
		responseVO.put("voList", voList);
		responseVO.put("totalElements", citiesPage.getTotalElements());
		return new ResponseEntity<>(responseVO, HttpStatus.OK);
	}

	@Override
	public Object getAllCities() {
		List<CitiesVO> voList = citiesRepo.findAll().stream().map(this::mapCitiesEntityToVo).toList();
		return new ResponseEntity<>(voList, HttpStatus.OK);
	}

}
