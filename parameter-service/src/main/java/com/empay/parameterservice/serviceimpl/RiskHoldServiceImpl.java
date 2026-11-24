package com.empay.parameterservice.serviceimpl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.empay.parameterservice.entities.RiskHoldEntity;
import com.empay.parameterservice.exceptions.ResourceAlreadyExistsException;
import com.empay.parameterservice.exceptions.ResourceNotFoundException;
import com.empay.parameterservice.exceptions.ServiceException;
import com.empay.parameterservice.repository.RiskHoldRepository;
import com.empay.parameterservice.service.RiskHoldService;
import com.empay.parameterservice.utils.ExceptionUtils;
import com.empay.parameterservice.valueobjects.RiskHoldVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class RiskHoldServiceImpl implements RiskHoldService {

	private final RiskHoldRepository riskHoldRepository;

	private static final Logger logger = LoggerFactory.getLogger(RiskHoldServiceImpl.class);

	private static final String RISK_HOLD = "Risk Hold";
	private static final String SERIAL_NUMBER = "serialNumber";

	private static final String NOT_FOUND = " not found.";
	private static final String RISK_HOLD_WITH_ID = "Risk hold with id ";

	public ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search) {
		Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

		Integer defaultPageNumber = Objects.nonNull(page) ? page - 1 : 0;
		Pageable pageable = PageRequest.of(defaultPageNumber, size, sort);
		Page<RiskHoldEntity> riskholdPage;
		if (search != null && !search.trim().isEmpty()) {
			Specification<RiskHoldEntity> specification = (root, query, cb) -> cb.or(
					cb.like(cb.lower(root.get("code")), "%" + search.toLowerCase() + "%"),
					cb.like(cb.lower(root.get("status")), "%" + search.toLowerCase() + "%"));
			riskholdPage = riskHoldRepository.findAll(specification, pageable);
		} else {
			riskholdPage = riskHoldRepository.findAll(pageable);
		}
		List<RiskHoldVO> voList = riskholdPage.getContent().stream().map(this::mapToRiskHoldVo).toList();

		Map<String, Object> responseVO = new HashMap<>();
		responseVO.put("voList", voList);
		responseVO.put("totalElements", riskholdPage.getTotalElements());
		return new ResponseEntity<>(responseVO, HttpStatus.OK);
	}

	private RiskHoldVO mapToRiskHoldVo(RiskHoldEntity entity) {
		try {
			if (entity == null)
				return null;

			RiskHoldVO vo = new RiskHoldVO();
			vo.setSerialNumber(entity.getSerialNumber());
			vo.setLastUpdatedDate(entity.getLastUpdatedDate() != null
					? entity.getLastUpdatedDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
					: null);
			vo.setUpdatedUser(entity.getUpdatedUser());
			vo.setInstitutionCode(entity.getInstitutionCode());
			vo.setStatus(entity.getStatus());
			vo.setCode(entity.getCode());
			vo.setDescription(entity.getDescription());
			vo.setTimeLimit(entity.getTimeLimit());
			vo.setCountLimit(entity.getCountLimit());
			vo.setAmountLimit(entity.getAmountLimit());
			vo.setEarlyTime(entity.getEarlyTime());
			vo.setLateTime(entity.getLateTime());
			vo.setPgTimeLimit(entity.getPgTimeLimit());
			vo.setPgCountLimit(entity.getPgCountLimit());
			vo.setPgAmountLimit(entity.getPgAmountLimit());
			vo.setPgEarlyTime(entity.getPgEarlyTime());
			vo.setPgLateTime(entity.getPgLateTime());

			return vo;
		} catch (Exception e) {
			logger.error("Error : mapToRiskHoldVo(): ", e);
			return new RiskHoldVO();
		}
	}

	public ResponseEntity<Object> findById(Integer id) {
		RiskHoldVO riskhold = this.mapToRiskHoldVo(riskHoldRepository.findById(id).orElseThrow(
				() -> new ResourceNotFoundException(RISK_HOLD, SERIAL_NUMBER, RISK_HOLD_WITH_ID + id + NOT_FOUND)));

		return new ResponseEntity<>(riskhold, HttpStatus.OK);
	}

	public String saveRiskHold(RiskHoldVO riskHoldVO) {
		if (riskHoldVO.getSerialNumber() != null && riskHoldRepository.existsById(riskHoldVO.getSerialNumber())) {
			throw new ResourceAlreadyExistsException(RISK_HOLD, SERIAL_NUMBER,
					RISK_HOLD_WITH_ID + riskHoldVO.getSerialNumber() + " already exists");
		}
		try {
			RiskHoldEntity entity = this.mapToRiskHoldEntity(riskHoldVO);
			riskHoldRepository.save(Objects.requireNonNull(entity));
			return "Saved Successfully";
		} catch (DataIntegrityViolationException ex) {
			log.error("Error saveRiskHold() ", ex);
			String constraint = ExceptionUtils.extractConstraintName(ex);
			throw mapConstraintToException(constraint);
		} catch (Exception ex) {
			log.error("Error saveRiskHold() ", ex);
			throw new ServiceException("Failed to save", ex);
		}
	}

	private RiskHoldEntity mapToRiskHoldEntity(RiskHoldVO vo) {
		try {
			if (vo == null)
				return null;

			RiskHoldEntity entity = new RiskHoldEntity();
			entity.setSerialNumber(vo.getSerialNumber());
			entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
			entity.setUpdatedUser(vo.getUpdatedUser());
			entity.setInstitutionCode(vo.getInstitutionCode());
			entity.setStatus(vo.getStatus());
			entity.setCode(vo.getCode());
			entity.setDescription(vo.getDescription());
			entity.setTimeLimit(vo.getTimeLimit());
			entity.setCountLimit(vo.getCountLimit());
			entity.setAmountLimit(vo.getAmountLimit());
			entity.setEarlyTime(vo.getEarlyTime());
			entity.setLateTime(vo.getLateTime());
			entity.setPgTimeLimit(vo.getPgTimeLimit());
			entity.setPgCountLimit(vo.getPgCountLimit());
			entity.setPgAmountLimit(vo.getPgAmountLimit());
			entity.setPgEarlyTime(vo.getPgEarlyTime());
			entity.setPgLateTime(vo.getPgLateTime());

			return entity;
		} catch (Exception e) {
			logger.error("Error :  mapToRiskHoldEntity(): ", e);
			return new RiskHoldEntity();
		}
	}

	public String updateRiskHold(RiskHoldVO riskHoldVO, Integer id) {

		if (riskHoldVO.getSerialNumber() != null && !id.equals(riskHoldVO.getSerialNumber())) {
			throw new IllegalArgumentException("Invalid request. Please verify the submitted details.");
		}
		RiskHoldEntity entity = riskHoldRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(RISK_HOLD, SERIAL_NUMBER,
						RISK_HOLD_WITH_ID + id + " not found"));
		try {
			this.updateRiskHoldEntity(entity, riskHoldVO);
			riskHoldRepository.save(entity);
			return "Updated Successfully";
		} catch (DataIntegrityViolationException ex) {
			log.error("Error updateRiskHold() ", ex);
			String constraint = ExceptionUtils.extractConstraintName(ex);
			throw mapConstraintToException(constraint);
		} catch (Exception ex) {
			log.error("Error updateRiskHold() ", ex);
			throw new ServiceException("Failed to update", ex);
		}
	}

	private void updateRiskHoldEntity(RiskHoldEntity entity, RiskHoldVO vo) {
		try {
			if (entity == null || vo == null)
				return;
			entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
			entity.setUpdatedUser(vo.getUpdatedUser());
			entity.setInstitutionCode(vo.getInstitutionCode());
			entity.setStatus(vo.getStatus());
			entity.setCode(vo.getCode());
			entity.setDescription(vo.getDescription());
			entity.setTimeLimit(vo.getTimeLimit());
			entity.setCountLimit(vo.getCountLimit());
			entity.setAmountLimit(vo.getAmountLimit());
			entity.setEarlyTime(vo.getEarlyTime());
			entity.setLateTime(vo.getLateTime());
			entity.setPgTimeLimit(vo.getPgTimeLimit());
			entity.setPgCountLimit(vo.getPgCountLimit());
			entity.setPgAmountLimit(vo.getPgAmountLimit());
			entity.setPgEarlyTime(vo.getPgEarlyTime());
			entity.setPgLateTime(vo.getPgLateTime());
		} catch (Exception e) {
			logger.error("Error :  updateRiskHoldEntity(): ", e);
		}
	}

	public String deleteRiskHold(Integer id) {
		if (!riskHoldRepository.existsById(id)) {
			throw new ResourceNotFoundException(RISK_HOLD, SERIAL_NUMBER, RISK_HOLD_WITH_ID + id + NOT_FOUND);
		}
		try {
			riskHoldRepository.deleteById(id);
			return "Deleted Successfully";
		} catch (Exception ex) {
			log.error("Error deleteRiskHold() ", ex);
			throw new ServiceException("Failed to delete", ex);
		}
	}

	private RuntimeException mapConstraintToException(String constraint) {
		if (constraint == null) {
			return new ResourceAlreadyExistsException(RISK_HOLD, "unknown", "Error");
		}
		return switch (constraint) {
		case "UK_RAH_CODE" ->
			new ResourceAlreadyExistsException(RISK_HOLD, "code", "Risk hold code already exists for this institution");
		case "PK_RAH_SER_NUMBER" ->
			new ResourceAlreadyExistsException(RISK_HOLD, SERIAL_NUMBER, "Risk hold serial number already exists");
		default -> new ResourceAlreadyExistsException(RISK_HOLD, "unknown", "Error");
		};
	}
}
