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

import com.empay.parameterservice.entities.PinCodeEntity;
import com.empay.parameterservice.exceptions.ResourceAlreadyExistsException;
import com.empay.parameterservice.exceptions.ResourceNotFoundException;
import com.empay.parameterservice.exceptions.ServiceException;
import com.empay.parameterservice.repository.PinCodeRepository;
import com.empay.parameterservice.service.PinCodeService;
import com.empay.parameterservice.utils.ExceptionUtils;
import com.empay.parameterservice.valueobjects.PinCodeVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class PinCodeServiceImpl implements PinCodeService {

	private final PinCodeRepository pinCodeRepository;

	private static final Logger logger = LoggerFactory.getLogger(PinCodeServiceImpl.class);

	private static final String PIN_CODE = "PinCode";
	private static final String SERIAL_NUMBER = "serialNumber";
	private static final String PIN_CODE_WITH_ID = "Pincode with id ";
	private static final String NOT_FOUND = " not found.";

	public ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search) {
		Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

		Integer defaultPageNumber = Objects.nonNull(page) ? page - 1 : 0;
		Pageable pageable = PageRequest.of(defaultPageNumber, size, sort);
		Page<PinCodeEntity> pincodePage;
		if (search != null && !search.trim().isEmpty()) {
			Specification<PinCodeEntity> specification = (root, query, cb) -> cb.or(
					cb.like(cb.lower(root.get("pinCode")), "%" + search.toLowerCase() + "%"),
					cb.like(cb.lower(root.get("pinStateName")), "%" + search.toLowerCase() + "%"),
					cb.like(cb.lower(root.get("pinCity")), "%" + search.toLowerCase() + "%"));
			pincodePage = pinCodeRepository.findAll(specification, pageable);
		} else {
			pincodePage = pinCodeRepository.findAll(pageable);
		}
		List<PinCodeVO> voList = pincodePage.getContent().stream().map(this::mapToPinCodesVo).toList();

		Map<String, Object> responseVO = new HashMap<>();
		responseVO.put("voList", voList);
		responseVO.put("totalElements", pincodePage.getTotalElements());
		return new ResponseEntity<>(responseVO, HttpStatus.OK);
	}

	private PinCodeVO mapToPinCodesVo(PinCodeEntity entity) {
		try {
			if (entity == null)
				return null;

			PinCodeVO vo = new PinCodeVO();
			vo.setSerialNumber(entity.getSerialNumber());
			vo.setLastUpdatedDate(entity.getLastUpdatedDate() != null
					? entity.getLastUpdatedDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
					: null);
			vo.setUpdatedUser(entity.getUpdatedUser());
			vo.setPinCode(entity.getPinCode());
			vo.setPinStateName(entity.getPinStateName());
			vo.setPinCity(entity.getPinCity());
			vo.setPinStateAlCode(entity.getPinStateAlCode());
			vo.setPinState(entity.getPinState());
			vo.setPinConCode(entity.getPinConCode());

			return vo;
		} catch (Exception e) {
			logger.error("Error: mapToPinCodesVo() : ", e);
			return new PinCodeVO();
		}
	}

	public ResponseEntity<Object> findById(Integer id) {
		PinCodeVO pincode = this.mapToPinCodesVo(pinCodeRepository.findById(id).orElseThrow(
				() -> new ResourceNotFoundException(PIN_CODE, SERIAL_NUMBER, PIN_CODE_WITH_ID + id + NOT_FOUND)));

		return new ResponseEntity<>(pincode, HttpStatus.OK);
	}

	public String savePincode(PinCodeVO pinCodeVO) {
		if (pinCodeVO.getSerialNumber() != null && pinCodeRepository.existsById(pinCodeVO.getSerialNumber())) {
			throw new ResourceAlreadyExistsException(PIN_CODE, SERIAL_NUMBER,
					"Pincode with code " + pinCodeVO.getSerialNumber() + " already exists");
		}
		try {
			PinCodeEntity entity = this.mapToPinCodesEntity(pinCodeVO);
			pinCodeRepository.save(entity);
			return "Saved Successfully";
		} catch (DataIntegrityViolationException ex) {
			log.error("Error savePincode() ", ex);
			String constraint = ExceptionUtils.extractConstraintName(ex);
			throw mapConstraintToException(constraint);
		} catch (Exception ex) {
			log.error("Error savePincode() ", ex);
			throw new ServiceException("Failed to save", ex);
		}
	}

	private PinCodeEntity mapToPinCodesEntity(PinCodeVO vo) {
		try {
			if (vo == null)
				return null;

			PinCodeEntity entity = new PinCodeEntity();
			entity.setSerialNumber(vo.getSerialNumber());
			entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
			entity.setUpdatedUser(vo.getUpdatedUser());
			entity.setPinCode(vo.getPinCode());
			entity.setPinStateName(vo.getPinStateName());
			entity.setPinCity(vo.getPinCity());
			entity.setPinStateAlCode(vo.getPinStateAlCode());
			entity.setPinState(vo.getPinState());
			entity.setPinConCode(vo.getPinConCode());
			return entity;
		} catch (Exception e) {
			logger.error("Error: mapToPinCodesEntity(): ", e);
			return new PinCodeEntity();
		}
	}

	public String updatePincode(PinCodeVO pinCodeVO, Integer id) {

		if (pinCodeVO.getSerialNumber() != null && !id.equals(pinCodeVO.getSerialNumber())) {
			throw new IllegalArgumentException("Invalid request. Please verify the submitted details.");
		}
		PinCodeEntity entity = pinCodeRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(PIN_CODE, SERIAL_NUMBER,
						PIN_CODE_WITH_ID + id + NOT_FOUND));
		try {
			this.mapUpdateEntity(entity, pinCodeVO);
			pinCodeRepository.save(entity);
			return "Updated Successfully";
		} catch (DataIntegrityViolationException ex) {
			log.error("Error updatePincode() ", ex);
			String constraint = ExceptionUtils.extractConstraintName(ex);
			throw mapConstraintToException(constraint);
		} catch (Exception ex) {
			log.error("Error updatePincode() ", ex);
			throw new ServiceException("Failed to update", ex);
		}
	}

	private void mapUpdateEntity(PinCodeEntity entity, PinCodeVO vo) {
		try {
			if (entity == null || vo == null)
				return;
			entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
			entity.setUpdatedUser(vo.getUpdatedUser());
			entity.setPinCode(vo.getPinCode());
			entity.setPinStateName(vo.getPinStateName());
			entity.setPinCity(vo.getPinCity());
			entity.setPinStateAlCode(vo.getPinStateAlCode());
			entity.setPinState(vo.getPinState());
			entity.setPinConCode(vo.getPinConCode());
		} catch (Exception e) {
			logger.error("Error: mapUpdateEntity(): ", e);
		}
	}

	public String deletePincode(Integer id) {
		if (!pinCodeRepository.existsById(id)) {
			throw new ResourceNotFoundException(PIN_CODE, SERIAL_NUMBER, PIN_CODE_WITH_ID + id + NOT_FOUND);
		}
		try {
			pinCodeRepository.deleteById(id);
			return "Deleted Successfully";
		} catch (Exception ex) {
			log.error("Error deletePincode() ", ex);
			throw new ServiceException("Failed to delete", ex);
		}
	}

	private RuntimeException mapConstraintToException(String constraint) {
		if (constraint == null) {
			return new ResourceAlreadyExistsException(PIN_CODE, "unknown", "Error");
		}
		return switch (constraint) {
		case "UK_PIN_CODE" -> new ResourceAlreadyExistsException(PIN_CODE, "pinCode", "Pincode already exists");
		case "PK_PIN_SER_NUMBER" ->
			new ResourceAlreadyExistsException(PIN_CODE, SERIAL_NUMBER, "Pincode serial number already exists");
		default -> new ResourceAlreadyExistsException(PIN_CODE, "unknown", "Error");
		};
	}
}
