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
import com.empay.parameterservice.entities.TerminalMakeEntity;
import com.empay.parameterservice.exceptions.ResourceAlreadyExistsException;
import com.empay.parameterservice.exceptions.ResourceNotFoundException;
import com.empay.parameterservice.exceptions.ServiceException;
import com.empay.parameterservice.repository.InstitutionsRepository;
import com.empay.parameterservice.repository.TerminalMakeRepository;
import com.empay.parameterservice.service.TerminalMakeService;
import com.empay.parameterservice.utils.ExceptionUtils;
import com.empay.parameterservice.valueobjects.TerminalMakeVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class TerminalMakeServiceImpl implements TerminalMakeService {

	private final TerminalMakeRepository terminalMakeRepository;
	private final InstitutionsRepository insRepo;

	private static final String TERMINAL_MAKE = "Terminal Make";
	private static final String SERIAL_NUMBER = "SerialNumber";
	private static final String TERMINAL_MAKE_WITH_ID = "Terminal make with id ";
	private static final String NOT_FOUND = " not found.";

	private static final Logger logger = LoggerFactory.getLogger(TerminalMakeServiceImpl.class);

	public ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search) {
		Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

		Integer defaultPageNumber = Objects.nonNull(page) ? page - 1 : 0;
		Pageable pageable = PageRequest.of(defaultPageNumber, size, sort);
		Page<TerminalMakeEntity> terminalMakePage;
		if (search != null && !search.trim().isEmpty()) {
			Specification<TerminalMakeEntity> specification = (root, query, cb) -> cb
					.or(cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"));
			terminalMakePage = terminalMakeRepository.findAll(specification, pageable);
		} else {
			terminalMakePage = terminalMakeRepository.findAll(pageable);
		}
		List<TerminalMakeVO> voList = terminalMakePage.getContent().stream().map(this::mapToTerminalMakeVo).toList();

		Map<String, Object> responseVO = new HashMap<>();
		responseVO.put("voList", voList);
		responseVO.put("totalElements", terminalMakePage.getTotalElements());
		return new ResponseEntity<>(responseVO, HttpStatus.OK);
	}

	private TerminalMakeVO mapToTerminalMakeVo(TerminalMakeEntity entity) {
		try {
			if (entity == null)
				return null;

			TerminalMakeVO vo = new TerminalMakeVO();
			vo.setSerialNumber(entity.getSerialNumber());
			vo.setLastUpdatedDate(entity.getLastUpdatedDate() != null
					? entity.getLastUpdatedDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
					: null);
			vo.setUpdatedUser(entity.getUpdatedUser());
			vo.setInstitutionCode(entity.getInsCode());
			vo.setName(entity.getName());
			vo.setInsName(Optional.ofNullable(entity.getInsCode()).map(insRepo::findByInstitutionCode)
					.map(InstitutionsEntity::getInstitutionName).orElse(null));
			return vo;
		} catch (Exception e) {
			logger.error("Error: mapToTerminalMakeVo() : ", e);
			return new TerminalMakeVO();
		}
	}

	public ResponseEntity<Object> findById(Integer id) {
		TerminalMakeVO terminalMake = this.mapToTerminalMakeVo(
				terminalMakeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(TERMINAL_MAKE,
						SERIAL_NUMBER, TERMINAL_MAKE_WITH_ID + id + NOT_FOUND)));

		return new ResponseEntity<>(terminalMake, HttpStatus.OK);
	}

	public String saveTerminalMake(TerminalMakeVO terminalMakeVO) {
		if (terminalMakeVO.getSerialNumber() != null
				&& terminalMakeRepository.existsById(terminalMakeVO.getSerialNumber())) {
			throw new ResourceAlreadyExistsException(TERMINAL_MAKE, SERIAL_NUMBER,
					TERMINAL_MAKE_WITH_ID + terminalMakeVO.getSerialNumber() + " already exists");
		}
		try {
			TerminalMakeEntity entity = this.mapToTerminalMakeEntity(terminalMakeVO);
			terminalMakeRepository.save(Objects.requireNonNull(entity));
			return "Saved Successfully";
		} catch (DataIntegrityViolationException ex) {
			log.error("Error saveTerminalMake() ", ex);
			String constraint = ExceptionUtils.extractConstraintName(ex);
			throw mapConstraintToException(constraint);
		} catch (Exception ex) {
			log.error("Error saveTerminalMake() ", ex);
			throw new ServiceException("Failed to save", ex);
		}
	}

	private TerminalMakeEntity mapToTerminalMakeEntity(TerminalMakeVO vo) {
		try {
			if (vo == null)
				return null;

			TerminalMakeEntity entity = new TerminalMakeEntity();
			entity.setSerialNumber(vo.getSerialNumber());
			entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
			entity.setUpdatedUser(vo.getUpdatedUser());
			entity.setInsCode(vo.getInstitutionCode());
			entity.setName(vo.getName());

			return entity;
		} catch (Exception e) {
			logger.error("Error mapToTerminalMakeEntity() ", e);
			return new TerminalMakeEntity();
		}
	}

	public String updateTerminalMake(TerminalMakeVO terminalMakeVO, Integer id) {

		if (terminalMakeVO.getSerialNumber() != null && !id.equals(terminalMakeVO.getSerialNumber())) {
			throw new IllegalArgumentException("Invalid request. Please verify the submitted details.");
		}
		TerminalMakeEntity entity = terminalMakeRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("TERMINAL_MAKE", "SERIAL_NUMBER",
						"TERMINAL_MAKE_WITH_ID " + id + " NOT_FOUND"));
		try {
			this.updateTerminalMakeEntity(entity, terminalMakeVO);
			terminalMakeRepository.save(entity);
			return "Updated Successfully";
		} catch (DataIntegrityViolationException ex) {
			log.error("Error updateTerminalMake() ", ex);
			String constraint = ExceptionUtils.extractConstraintName(ex);
			throw mapConstraintToException(constraint);
		} catch (Exception ex) {
			log.error("Error updateTerminalMake() ", ex);
			throw new ServiceException("Failed to update", ex);
		}
	}

	private void updateTerminalMakeEntity(TerminalMakeEntity entity, TerminalMakeVO vo) {
		try {
			if (entity == null || vo == null)
				return;
			entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
			entity.setUpdatedUser(vo.getUpdatedUser());
			entity.setInsCode(vo.getInstitutionCode());
			entity.setName(vo.getName());
		} catch (Exception e) {
			logger.error("Error updateTerminalMakeEntity() ", e);
		}
	}

	public String deleteTerminalMake(Integer id) {
		if (!terminalMakeRepository.existsById(id)) {
			throw new ResourceNotFoundException(TERMINAL_MAKE, SERIAL_NUMBER, TERMINAL_MAKE_WITH_ID + id + NOT_FOUND);
		}
		try {
			terminalMakeRepository.deleteById(id);
			return "Deleted Successfully";
		} catch (Exception ex) {
			log.error("Error deleteTerminalMake() ", ex);
			throw new ServiceException("Failed to delete", ex);
		}
	}

	private RuntimeException mapConstraintToException(String constraint) {
		if (constraint == null) {
			return new ResourceAlreadyExistsException(TERMINAL_MAKE, "unknown", "Error");
		}
		return switch (constraint) {
		case "UK_TMK_NAME" -> new ResourceAlreadyExistsException(TERMINAL_MAKE, "name",
				"Terminal make name already exists for this institution");
		case "PK_TMK_SER_NUMBER" -> new ResourceAlreadyExistsException(TERMINAL_MAKE, "serialNumber",
				"Terminal make serial number already exists");
		default -> new ResourceAlreadyExistsException(TERMINAL_MAKE, "unknown", "Error");
		};
	}

	@Override
	public ResponseEntity<Object> getAllTerminalMakesWithIns() {
		List<TerminalMakeVO> voList = terminalMakeRepository.findAll().stream().map(this::mapToTerminalMakeVo).toList();
		return new ResponseEntity<>(voList, HttpStatus.OK);
	}

}
