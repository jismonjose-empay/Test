package com.empay.parameterservice.serviceimpl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.empay.parameterservice.entities.RentalPlansEntity;
import com.empay.parameterservice.exceptions.ResourceAlreadyExistsException;
import com.empay.parameterservice.exceptions.ResourceNotFoundException;
import com.empay.parameterservice.exceptions.ServiceException;
import com.empay.parameterservice.repository.RentalPlansRepository;
import com.empay.parameterservice.service.RentalPlansService;
import com.empay.parameterservice.utils.ExceptionUtils;
import com.empay.parameterservice.utils.JwtUtils;
import com.empay.parameterservice.valueobjects.RentalPlansVO;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * Service implementation for rental plan operations and business logic.
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class RentalPlansServiceImpl implements RentalPlansService {

	private final RentalPlansRepository rentalPlansRepository;
	private final JwtUtils jwtUtils;

	private static final DateTimeFormatter DATE_FORMAT_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

	private static final String RENTAL_PLAN = "Rental Plan";
	private static final String SERIAL_NUMBER = "serialNumber";
	private static final String NOT_FOUND = " not found.";

	public static final int STATUS_ACTIVE = 1;
	public static final int STATUS_DELETED = 16;

	@Override
	public ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search,
			Integer institutionCode, Integer aggregatorSrlNo) {
		Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
		int defaultPageNumber = Objects.nonNull(page) && page > 0 ? page - 1 : 0;
		Pageable pageable = PageRequest.of(defaultPageNumber, size, sort);

		Specification<RentalPlansEntity> specification = (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(cb.notEqual(root.get("status"), STATUS_DELETED));

			if (institutionCode != null && institutionCode > 0) {
				predicates.add(cb.equal(root.get("institutionCode"), institutionCode));
			}

			if (aggregatorSrlNo != null && aggregatorSrlNo > 0) {
				predicates.add(cb.equal(root.get("aggregatorSrlNo"), aggregatorSrlNo));
			}

			if (search != null && !search.trim().isEmpty()) {
				String likePattern = "%" + search.toLowerCase().trim() + "%";
				predicates.add(cb.like(cb.lower(root.get("planDescription")), likePattern));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};

		Page<RentalPlansEntity> planPage = rentalPlansRepository.findAll(specification, pageable);
		List<RentalPlansVO> voList = planPage.getContent().stream().map(this::mapEntityToVO).toList();

		Map<String, Object> responseVO = new HashMap<>();
		responseVO.put("voList", voList);
		responseVO.put("totalElements", planPage.getTotalElements());
		return new ResponseEntity<>(responseVO, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<Object> findAllActive(Integer institutionCode, Integer aggregatorSrlNo) {
		List<RentalPlansEntity> entities;
		if (institutionCode != null && institutionCode > 0) {
			if (aggregatorSrlNo != null && aggregatorSrlNo > 0) {
				entities = rentalPlansRepository.findByInstitutionCodeAndAggregatorSrlNoAndStatusNot(institutionCode,
						aggregatorSrlNo, STATUS_DELETED);
			} else {
				entities = rentalPlansRepository.findByInstitutionCodeAndStatusNot(institutionCode, STATUS_DELETED);
			}
		} else {
			entities = rentalPlansRepository.findByStatusNot(STATUS_DELETED);
		}

		List<RentalPlansVO> voList = entities.stream().map(this::mapEntityToVO).toList();
		return new ResponseEntity<>(voList, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<Object> findById(Integer id) {
		RentalPlansEntity entity = rentalPlansRepository.findById(id).orElseThrow(
				() -> new ResourceNotFoundException(RENTAL_PLAN, SERIAL_NUMBER, "Rental Plan with id " + id + NOT_FOUND));

		if (entity.getStatus() != null && entity.getStatus() == STATUS_DELETED) {
			throw new ResourceNotFoundException(RENTAL_PLAN, SERIAL_NUMBER, "Rental Plan with id " + id + NOT_FOUND);
		}

		return new ResponseEntity<>(mapEntityToVO(entity), HttpStatus.OK);
	}

	@Override
	public String saveRentalPlan(RentalPlansVO rentalPlansVO, HttpServletRequest request) {
		if (rentalPlansVO.getSerialNumber() != null
				&& rentalPlansRepository.existsById(rentalPlansVO.getSerialNumber())) {
			throw new ResourceAlreadyExistsException(RENTAL_PLAN, SERIAL_NUMBER,
					"Rental Plan with id " + rentalPlansVO.getSerialNumber() + " already exists");
		}
		try {
			rentalPlansVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
			RentalPlansEntity entity = mapVOToEntity(rentalPlansVO);
			if (entity.getStatus() == null || entity.getStatus() == 0) {
				entity.setStatus(STATUS_ACTIVE);
			}
			rentalPlansRepository.saveAndFlush(entity);
			return "Saved Successfully";
		} catch (DataIntegrityViolationException ex) {
			log.error("Error saveRentalPlan() ", ex);
			String constraint = ExceptionUtils.extractConstraintName(ex);
			throw mapConstraintToException(constraint);
		} catch (Exception ex) {
			log.error("Error saveRentalPlan() ", ex);
			throw new ServiceException("Failed to save", ex);
		}
	}

	@Override
	public String updateRentalPlan(RentalPlansVO rentalPlansVO, Integer id, HttpServletRequest request) {
		if (rentalPlansVO.getSerialNumber() != null && !id.equals(rentalPlansVO.getSerialNumber())) {
			throw new IllegalArgumentException("Invalid request. Identifier in path and payload do not match.");
		}

		RentalPlansEntity existingEntity = rentalPlansRepository.findById(id).orElseThrow(
				() -> new ResourceNotFoundException(RENTAL_PLAN, SERIAL_NUMBER, "Rental Plan with id " + id + NOT_FOUND));

		try {
			rentalPlansVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
			updateEntity(existingEntity, rentalPlansVO);
			rentalPlansRepository.saveAndFlush(existingEntity);
			return "Updated Successfully";
		} catch (DataIntegrityViolationException ex) {
			log.error("Error updateRentalPlan() ", ex);
			String constraint = ExceptionUtils.extractConstraintName(ex);
			throw mapConstraintToException(constraint);
		} catch (Exception ex) {
			log.error("Error updateRentalPlan() ", ex);
			throw new ServiceException("Failed to update", ex);
		}
	}

	@Override
	public String deleteRentalPlan(Integer id, HttpServletRequest request) {
		RentalPlansEntity existingEntity = rentalPlansRepository.findById(id).orElseThrow(
				() -> new ResourceNotFoundException(RENTAL_PLAN, SERIAL_NUMBER, "Rental Plan with id " + id + NOT_FOUND));

		try {
			existingEntity.setStatus(STATUS_DELETED);
			existingEntity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
			existingEntity.setUpdatedUser(jwtUtils.getUserSerNumber(request));
			rentalPlansRepository.saveAndFlush(existingEntity);
			return "Deleted Successfully";
		} catch (Exception ex) {
			log.error("Error deleteRentalPlan() ", ex);
			throw new ServiceException("Failed to delete", ex);
		}
	}

	public RentalPlansVO mapEntityToVO(RentalPlansEntity entity) {
		if (entity == null) {
			return null;
		}
		RentalPlansVO vo = new RentalPlansVO();
		vo.setSerialNumber(entity.getSerialNumber());
		vo.setLastUpdatedDate(
				entity.getLastUpdatedDate() != null ? entity.getLastUpdatedDate().format(DATE_FORMAT_TIME) : null);
		vo.setUpdatedUser(entity.getUpdatedUser());
		vo.setInstitutionCode(entity.getInstitutionCode());
		vo.setStatus(entity.getStatus());
		vo.setPlanDescription(entity.getPlanDescription());
		vo.setRentalAmount(entity.getRentalAmount());
		vo.setRentalFrequency(entity.getRentalFrequency());
		vo.setFrequencyDescription(resolveFrequencyDescription(entity.getRentalFrequency()));
		vo.setAggregatorSrlNo(entity.getAggregatorSrlNo());
		return vo;
	}

	public RentalPlansEntity mapVOToEntity(RentalPlansVO vo) {
		if (vo == null) {
			return null;
		}
		RentalPlansEntity entity = new RentalPlansEntity();
		entity.setSerialNumber(vo.getSerialNumber());
		entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
		entity.setUpdatedUser(vo.getUpdatedUser());
		entity.setInstitutionCode(vo.getInstitutionCode());
		entity.setStatus(vo.getStatus() != null ? vo.getStatus() : STATUS_ACTIVE);
		entity.setPlanDescription(vo.getPlanDescription());
		entity.setRentalAmount(vo.getRentalAmount());
		entity.setRentalFrequency(vo.getRentalFrequency());
		entity.setAggregatorSrlNo(vo.getAggregatorSrlNo());
		return entity;
	}

	private void updateEntity(RentalPlansEntity entity, RentalPlansVO vo) {
		entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
		entity.setUpdatedUser(vo.getUpdatedUser());
		entity.setInstitutionCode(vo.getInstitutionCode());
		if (vo.getStatus() != null) {
			entity.setStatus(vo.getStatus());
		}
		entity.setPlanDescription(vo.getPlanDescription());
		entity.setRentalAmount(vo.getRentalAmount());
		entity.setRentalFrequency(vo.getRentalFrequency());
		entity.setAggregatorSrlNo(vo.getAggregatorSrlNo());
	}

	public static String resolveFrequencyDescription(Character freq) {
		if (freq == null) {
			return "";
		}
		return switch (Character.toUpperCase(freq)) {
			case 'D' -> "Daily";
			case 'M' -> "Monthly 1";
			case 'P' -> "Monthly 2";
			case 'Q' -> "Quarterly";
			case 'H' -> "Half Yearly";
			case 'Y' -> "Yearly";
			case 'L' -> "Life Time";
			default -> String.valueOf(freq);
		};
	}

	private RuntimeException mapConstraintToException(String constraint) {
		if (constraint == null) {
			return new ResourceAlreadyExistsException(RENTAL_PLAN, "unknown", "Data integrity violation");
		}
		return switch (constraint) {
			case "PK_RPL_SER_NUMBER" ->
				new ResourceAlreadyExistsException(RENTAL_PLAN, SERIAL_NUMBER, "Rental Plan serial number already exists");
			case "CK_RPL_FREQUENCY" ->
				new IllegalArgumentException("Invalid rental frequency value. Allowed values are D, M, P, Q, H, Y, L.");
			default -> new ResourceAlreadyExistsException(RENTAL_PLAN, "unknown", "Data integrity violation");
		};
	}
}
