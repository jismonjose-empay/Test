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

import com.empay.parameterservice.entities.BankNamesEntity;
import com.empay.parameterservice.entities.CitiesEntity;
import com.empay.parameterservice.entities.InstitutionsEntity;
import com.empay.parameterservice.entities.StatesEntity;
import com.empay.parameterservice.exceptions.ResourceAlreadyExistsException;
import com.empay.parameterservice.exceptions.ResourceNotFoundException;
import com.empay.parameterservice.exceptions.ServiceException;
import com.empay.parameterservice.repository.BankNamesRepository;
import com.empay.parameterservice.repository.CitiesRepo;
import com.empay.parameterservice.repository.InstitutionsRepository;
import com.empay.parameterservice.repository.StatesRepo;
import com.empay.parameterservice.service.BankNamesService;
import com.empay.parameterservice.utils.ExceptionUtils;
import com.empay.parameterservice.utils.JwtUtils;
import com.empay.parameterservice.valueobjects.BankNamesVO;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class BankNamesServiceImpl implements BankNamesService {

	private final BankNamesRepository bankNamesRepository;
	private final CitiesRepo citiesRepo;
	private final StatesRepo statesRepo;
	private final InstitutionsRepository insRepo;
	private final JwtUtils jwtUtils;

	private static final String BANKS = "Banks";
	private static final String SERIAL_NUMBER = "serialNumber";
	private static final String BANK_NAME_WITH_ID = "Bank name with id ";
	private static final String NOT_FOUND = " not found.";

	private static final DateTimeFormatter DateFormatTime = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

	public ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search) {
		Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

		Integer defaultPageNumber = Objects.nonNull(page) ? page - 1 : 0;
		Pageable pageable = PageRequest.of(defaultPageNumber, size, sort);
		Page<BankNamesEntity> bankNamesPage;
		if (search != null && !search.trim().isEmpty()) {
			Specification<BankNamesEntity> specification = (root, query, cb) -> cb
					.or(cb.like(cb.lower(root.get("bankName")), "%" + search.toLowerCase() + "%"));
			bankNamesPage = bankNamesRepository.findAll(specification, pageable);
		} else {
			bankNamesPage = bankNamesRepository.findAll(pageable);
		}
		List<BankNamesVO> voList = bankNamesPage.getContent().stream().map(this::mapBankNamesEntityToVO).toList();
		Map<String, Object> responseVO = new HashMap<>();
		responseVO.put("voList", voList);
		responseVO.put("totalElements", bankNamesPage.getTotalElements());
		return new ResponseEntity<>(responseVO, HttpStatus.OK);
	}

	public ResponseEntity<Object> findById(Integer id) {
		BankNamesVO bankNames = mapBankNamesEntityToVO(bankNamesRepository.findById(id).orElseThrow(
				() -> new ResourceNotFoundException(BANKS, SERIAL_NUMBER, BANK_NAME_WITH_ID + id + NOT_FOUND)));
		return ResponseEntity.ok(bankNames);
	}

	public BankNamesVO mapBankNamesEntityToVO(BankNamesEntity entity) {
		if (entity == null) {
			return null;
		}
		BankNamesVO vo = new BankNamesVO();
		vo.setSerialNumber(entity.getSerialNumber());
		vo.setLastUpdatedDate(
				entity.getLastUpdatedDate() != null ? entity.getLastUpdatedDate().format(DateFormatTime) : null);
		vo.setUpdatedUser(entity.getUpdatedUser());
		vo.setInstitutionCode(entity.getInstitutionCode());
		vo.setBankName(entity.getBankName());
		vo.setBankCity(entity.getBankCity());
		vo.setBankState(entity.getBankState());
		vo.setInsName(Optional.ofNullable(entity.getInstitutionCode()).map(insRepo::findByInstitutionCode)
				.map(InstitutionsEntity::getInstitutionName).orElse(null));

		vo.setBankCityName(Optional.ofNullable(entity.getBankCity()).map(citiesRepo::findBySerialNumber)
				.map(CitiesEntity::getCityName).orElse(null));

		vo.setBankStateName(Optional.ofNullable(entity.getBankState()).map(state -> String.format("%02d", state))
				.map(statesRepo::findByStateCode).map(StatesEntity::getStateName).orElse(null));
		return vo;
	}

	public String saveBankName(BankNamesVO bankNamesVO, HttpServletRequest request) {
		if (bankNamesVO.getSerialNumber() != null && bankNamesRepository.existsById(bankNamesVO.getSerialNumber())) {
			throw new ResourceAlreadyExistsException(BANKS, SERIAL_NUMBER,
					BANK_NAME_WITH_ID + bankNamesVO.getSerialNumber() + " already exists");
		}
		try {
			bankNamesVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
			BankNamesEntity entity = mapBankNamesVOToEntity(bankNamesVO);
			bankNamesRepository.saveAndFlush(entity);
			return "Saved Successfully";
		} catch (DataIntegrityViolationException ex) {
			log.error("Error saveBankName() ", ex);
			String constraint = ExceptionUtils.extractConstraintName(ex);
			throw mapConstraintToException(constraint);
		} catch (Exception ex) {
			log.error("Error saveBankName() ", ex);
			throw new ServiceException("Failed to save", ex);
		}
	}

	public BankNamesEntity mapBankNamesVOToEntity(BankNamesVO vo) {
		if (vo == null) {
			return null;
		}
		BankNamesEntity entity = new BankNamesEntity();
		entity.setSerialNumber(vo.getSerialNumber());
		entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
		entity.setUpdatedUser(vo.getUpdatedUser());
		entity.setInstitutionCode(vo.getInstitutionCode());
		entity.setBankName(vo.getBankName());
		entity.setBankCity(vo.getBankCity());
		entity.setBankState(vo.getBankState());
		return entity;
	}

	private void updateBankNamesEntity(BankNamesEntity entity, BankNamesVO vo) {

		entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
		entity.setUpdatedUser(vo.getUpdatedUser());
		entity.setInstitutionCode(vo.getInstitutionCode());
		entity.setBankName(vo.getBankName());
		entity.setBankCity(vo.getBankCity());
		entity.setBankState(vo.getBankState());
	}

	public String updateBankName(BankNamesVO bankNamesVO, Integer id, HttpServletRequest request) {
		if (bankNamesVO.getSerialNumber() != null && !id.equals(bankNamesVO.getSerialNumber())) {
			throw new IllegalArgumentException("Invalid request. Please verify the submitted details.");
		}
		BankNamesEntity existingEntity = bankNamesRepository.findById(id).orElseThrow(
				() -> new ResourceNotFoundException(BANKS, SERIAL_NUMBER, BANK_NAME_WITH_ID + id + NOT_FOUND));
		try {
			bankNamesVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
			updateBankNamesEntity(existingEntity, bankNamesVO);
			bankNamesRepository.save(existingEntity);
			return "Updated Successfully";
		} catch (DataIntegrityViolationException ex) {
			log.error("Error updateBankName() ", ex);
			String constraint = ExceptionUtils.extractConstraintName(ex);
			throw mapConstraintToException(constraint);
		} catch (Exception ex) {
			log.error("Error updateBankName() ", ex);
			throw new ServiceException("Failed to update", ex);
		}
	}

	public String deleteBankName(Integer id) {
		if (!bankNamesRepository.existsById(id)) {
			throw new ResourceNotFoundException(BANKS, SERIAL_NUMBER, BANK_NAME_WITH_ID + id + NOT_FOUND);
		}
		try {
			bankNamesRepository.deleteById(id);
			return "Deleted Successfully";
		} catch (Exception ex) {
			log.error("Error deleteBankName() ", ex);
			throw new ServiceException("Failed to delete", ex);
		}
	}

	private RuntimeException mapConstraintToException(String constraint) {
		if (constraint == null) {
			return new ResourceAlreadyExistsException(BANKS, "unknown", "Error");
		}
		return switch (constraint) {
		case "PK_CON_CODE" ->
			new ResourceAlreadyExistsException(BANKS, SERIAL_NUMBER, "Bank serial number already exists");
		default -> new ResourceAlreadyExistsException(BANKS, "unknown", "Error");
		};
	}

	@Override
	public ResponseEntity<Object> getAllBankNames() {
		var entity = bankNamesRepository.findAll();
		List<BankNamesVO> voList = entity.stream().map(this::mapBankNamesEntityToVO).toList();
		return ResponseEntity.ok(voList);
	}
}
