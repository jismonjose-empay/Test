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

import com.empay.parameterservice.entities.InstitutionsEntity;
import com.empay.parameterservice.entities.TerminalModelEntity;
import com.empay.parameterservice.exceptions.ResourceAlreadyExistsException;
import com.empay.parameterservice.exceptions.ResourceNotFoundException;
import com.empay.parameterservice.exceptions.ServiceException;
import com.empay.parameterservice.repository.InstitutionsRepository;
import com.empay.parameterservice.repository.TerminalMakeRepository;
import com.empay.parameterservice.repository.TerminalModelRepository;
import com.empay.parameterservice.service.TerminalModelService;
import com.empay.parameterservice.utils.ExceptionUtils;
import com.empay.parameterservice.valueobjects.TerminalModelVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class TerminalModelServiceImpl implements TerminalModelService {

	private final TerminalModelRepository terminalModelRepository;
	private final TerminalMakeRepository terminalMakeRepository;
	private final InstitutionsRepository insRepo;

	private static final String TERMINAL_MODEL = "Terminal Model";
	private static final String SERIAL_NUMBER = "serialNumber";
	private static final String TERMINAL_WITH_ID = "Terminal model with id ";
	private static final String NOT_FOUND = " not found.";
	private static final String MODEL = "model";

	private static final Logger logger = LoggerFactory.getLogger(TerminalModelServiceImpl.class);

	public ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search) {
		String sortProperty = switch (sortBy) {
		case "makeName" -> "make.name";
		case MODEL -> MODEL;
		case "mode" -> "mode";
		default -> SERIAL_NUMBER;
		};

		Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortProperty).descending()
				: Sort.by(sortProperty).ascending();

		Integer defaultPageNumber = Objects.nonNull(page) ? page - 1 : 0;
		Pageable pageable = PageRequest.of(defaultPageNumber, size, sort);
		Page<TerminalModelEntity> terminalModelPage;
		if (search != null && !search.trim().isEmpty()) {
			Specification<TerminalModelEntity> specification = (root, query, cb) -> cb.or(
					cb.like(cb.lower(root.get(MODEL)), "%" + search.toLowerCase() + "%"),
					cb.like(cb.lower(root.get("mode")), "%" + search.toLowerCase() + "%"),
					cb.like(cb.lower(root.get("make").get("name")), "%" + search.toLowerCase() + "%"));
			terminalModelPage = terminalModelRepository.findAll(specification, pageable);
		} else {
			terminalModelPage = terminalModelRepository.findAll(pageable);
		}
		List<TerminalModelVO> voList = terminalModelPage.getContent().stream().map(this::mapToTerminalModelVO).toList();

		Map<String, Object> responseVO = new HashMap<>();
		responseVO.put("voList", voList);
		responseVO.put("totalElements", terminalModelPage.getTotalElements());
		return new ResponseEntity<>(responseVO, HttpStatus.OK);
	}

	private TerminalModelVO mapToTerminalModelVO(TerminalModelEntity entity) {
		try {
			if (entity == null)
				return null;

			TerminalModelVO vo = new TerminalModelVO();
			vo.setSerialNumber(entity.getSerialNumber());
			vo.setLastUpdatedDate(entity.getLastUpdatedDate() != null
					? entity.getLastUpdatedDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
					: null);
			vo.setUpdatedUser(entity.getUpdatedUser());
			vo.setInstitutionCode(entity.getInsCode());
			vo.setMakeName(entity.getMake().getName());
			vo.setMake(entity.getMake().getSerialNumber());
			vo.setModel(entity.getModel());
			vo.setMode(entity.getMode());
			vo.setInsName(Optional.ofNullable(entity.getInsCode()).map(insRepo::findByInstitutionCode)
					.map(InstitutionsEntity::getInstitutionName).orElse(null));
			return vo;
		} catch (Exception e) {
			logger.error("Error in mapToTerminalModelVO", e);
			return new TerminalModelVO();
		}
	}

	public ResponseEntity<Object> findById(Integer id) {
		TerminalModelVO terminalModel = this.mapToTerminalModelVO(terminalModelRepository.findById(id).orElseThrow(
				() -> new ResourceNotFoundException(TERMINAL_MODEL, SERIAL_NUMBER, TERMINAL_WITH_ID + id + NOT_FOUND)));
		return new ResponseEntity<>(terminalModel, HttpStatus.OK);
	}

	public String saveTerminalModel(TerminalModelVO terminalModelVO) {
		if (terminalModelVO.getSerialNumber() != null
				&& terminalModelRepository.existsById(terminalModelVO.getSerialNumber())) {
			throw new ResourceAlreadyExistsException(TERMINAL_MODEL, SERIAL_NUMBER,
					TERMINAL_WITH_ID + terminalModelVO.getSerialNumber() + " already exists");
		}
		try {
			TerminalModelEntity entity = this.mapToTerminalModelEntity(terminalModelVO);
			terminalModelRepository.save(Objects.requireNonNull(entity));
			return "Saved Successfully";
		} catch (DataIntegrityViolationException ex) {
			log.error("Error saveTerminalModel() ", ex);
			String constraint = ExceptionUtils.extractConstraintName(ex);
			throw mapConstraintToException(constraint);
		} catch (Exception ex) {
			log.error("Error saveTerminalModel() ", ex);
			throw new ServiceException("Failed to save", ex);
		}
	}

	private TerminalModelEntity mapToTerminalModelEntity(TerminalModelVO vo) {
		try {
			if (vo == null)
				return null;

			TerminalModelEntity entity = new TerminalModelEntity();
			entity.setSerialNumber(vo.getSerialNumber());
			entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
			entity.setUpdatedUser(vo.getUpdatedUser());
			entity.setInsCode(vo.getInstitutionCode() != null ? vo.getInstitutionCode() : null);
			entity.setMake(vo.getMake() != null ? terminalMakeRepository.findById(vo.getMake())
					.orElseThrow(() -> new ResourceNotFoundException("TerminalModel", "make",
							"Terminal make not found: " + vo.getMake()))
					: null);
			entity.setModel(vo.getModel());
			entity.setMode(vo.getMode());

			return entity;
		} catch (Exception e) {
			logger.error("Error mapToTerminalModelEntity() ", e);
			return new TerminalModelEntity();
		}
	}

	public String updateTerminalModel(TerminalModelVO terminalModelVO, Integer id) {
		if (terminalModelVO == null) {
			throw new IllegalArgumentException("Invalid terminal model data");
		}
		if (terminalModelVO.getSerialNumber() != null && !id.equals(terminalModelVO.getSerialNumber())) {
			throw new IllegalArgumentException("Invalid request. Please verify the submitted details.");
		}
		TerminalModelEntity entity = terminalModelRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(TERMINAL_MODEL, SERIAL_NUMBER,
						TERMINAL_WITH_ID  + id +  NOT_FOUND));
		try {
			this.updateTerminalModelEntity(entity, terminalModelVO);
			terminalModelRepository.save(entity);
			return "Updated Successfully";
		} catch (DataIntegrityViolationException ex) {
			log.error("Error updateTerminalModel() ", ex);
			String constraint = ExceptionUtils.extractConstraintName(ex);
			throw mapConstraintToException(constraint);
		} catch (ResourceNotFoundException | IllegalArgumentException ex) {
			throw ex;
		} catch (Exception ex) {
			log.error("Error updateTerminalModel() ", ex);
			throw new ServiceException("Failed to update", ex);
		}
	}

	private void updateTerminalModelEntity(TerminalModelEntity entity, TerminalModelVO vo) {

		if (entity == null || vo == null) {
			return;
		}

		entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
		entity.setUpdatedUser(vo.getUpdatedUser());
		entity.setInsCode(vo.getInstitutionCode());
		entity.setMake(vo.getMake() != null ? terminalMakeRepository.findById(vo.getMake())
				.orElseThrow(() -> new ResourceNotFoundException("TerminalModel", "make",
						"Terminal make not found: " + vo.getMake()))
				: null);
		entity.setModel(vo.getModel());
		entity.setMode(vo.getMode());

	}

	public String deleteTerminalModel(Integer id) {
		if (!terminalModelRepository.existsById(id)) {
			throw new ResourceNotFoundException(TERMINAL_MODEL, SERIAL_NUMBER, TERMINAL_WITH_ID + id + NOT_FOUND);
		}
		try {
			terminalModelRepository.deleteById(id);
			return "Deleted Successfully";
		} catch (Exception ex) {
			log.error("Error deleteTerminalModel() ", ex);
			throw new ServiceException("Failed to delete", ex);
		}
	}

	private RuntimeException mapConstraintToException(String constraint) {
		if (constraint == null) {
			return new ResourceAlreadyExistsException(TERMINAL_MODEL, "unknown", "Error");
		}
		return switch (constraint) {
		case "UK_TMD_MODEL" -> new ResourceAlreadyExistsException(TERMINAL_MODEL, "name",
				"Terminal model/mode already exists for this Terminal make");
		case "PK_TMD_SER_NUMBER" -> new ResourceAlreadyExistsException(TERMINAL_MODEL, SERIAL_NUMBER,
				"Terminal model serial number already exists");
		default -> new ResourceAlreadyExistsException(TERMINAL_MODEL, "unknown", "Error");
		};
	}
}
