package com.empay.parameterservice.serviceimpl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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

import com.empay.parameterservice.entities.CurrenciesEntity;
import com.empay.parameterservice.exceptions.ResourceAlreadyExistsException;
import com.empay.parameterservice.exceptions.ResourceNotFoundException;
import com.empay.parameterservice.exceptions.ServiceException;
import com.empay.parameterservice.repository.CurrenciesRepository;
import com.empay.parameterservice.service.CurrenciesService;
import com.empay.parameterservice.utils.ExceptionUtils;
import com.empay.parameterservice.utils.JwtUtils;
import com.empay.parameterservice.valueobjects.CurrenciesVO;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class CurrenciesServiceImpl implements CurrenciesService {

	private final CurrenciesRepository currenciesRepository;
	private final JwtUtils jwtUtils;

	private static final String CURR_CODE = "currencyCode";
	private static final String CURR_WITH_CODE = "Currency with code ";
	private static final String CURRENCY = "Currency";
	private static final String NOT_FOUND = " not found.";

	private static final DateTimeFormatter DateFormatTime = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

	public ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search) {
		Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

		Integer defaultPageNumber = Objects.nonNull(page) ? page - 1 : 0;
		Pageable pageable = PageRequest.of(defaultPageNumber, size, sort);
		Page<CurrenciesEntity> currenciesPage;
		if (search != null && !search.trim().isEmpty()) {
			Specification<CurrenciesEntity> specification = (root, query, cb) -> cb.or(
					cb.like(cb.lower(root.get(CURR_CODE)), "%" + search.toLowerCase() + "%"),
					cb.like(cb.lower(root.get("currencyName")), "%" + search.toLowerCase() + "%"),
					cb.like(cb.lower(root.get("alphaCode")), "%" + search.toLowerCase() + "%"));
			currenciesPage = currenciesRepository.findAll(specification, pageable);
		} else {
			currenciesPage = currenciesRepository.findAll(pageable);
		}
		List<CurrenciesVO> voList = currenciesPage.getContent().stream().map(this::mapCurrenciesEntityToVO).toList();
		Map<String, Object> responseVO = new HashMap<>();
		responseVO.put("voList", voList);
		responseVO.put("totalElements", currenciesPage.getTotalElements());
		return new ResponseEntity<>(responseVO, HttpStatus.OK);
	}

	public ResponseEntity<Object> findById(String code) {
		CurrenciesVO currencies = mapCurrenciesEntityToVO(currenciesRepository.findById(code).orElseThrow(
				() -> new ResourceNotFoundException(CURRENCY, CURR_CODE, CURR_WITH_CODE + code + NOT_FOUND)));
		return ResponseEntity.ok(currencies);
	}

	public CurrenciesVO mapCurrenciesEntityToVO(CurrenciesEntity entity) {
		if (entity == null) {
			return null;
		}
		CurrenciesVO vo = new CurrenciesVO();
		vo.setCurrencyCode(entity.getCurrencyCode());
		vo.setLastUpdatedDate(
				entity.getLastUpdatedDate() != null ? entity.getLastUpdatedDate().format(DateFormatTime) : null);
		vo.setUpdatedUser(entity.getUpdatedUser());
		vo.setAlphaCode(entity.getAlphaCode());
		vo.setExponent(entity.getExponent());
		vo.setCurrencyName(entity.getCurrencyName());
		return vo;
	}

	public CurrenciesEntity mapCurrenciesVOToEntity(CurrenciesVO vo) {
		if (vo == null) {
			return null;
		}
		CurrenciesEntity entity = new CurrenciesEntity();
		entity.setCurrencyCode(vo.getCurrencyCode());
		entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
		entity.setUpdatedUser(vo.getUpdatedUser());
		entity.setAlphaCode(vo.getAlphaCode());
		entity.setExponent(vo.getExponent());
		entity.setCurrencyName(vo.getCurrencyName());
		return entity;
	}

	private void updateCurrenciesEntity(CurrenciesEntity entity, CurrenciesVO vo) {

		entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
		entity.setUpdatedUser(vo.getUpdatedUser());
		entity.setAlphaCode(vo.getAlphaCode());
		entity.setExponent(vo.getExponent());
		entity.setCurrencyName(vo.getCurrencyName());
	}

	public String saveCurrency(CurrenciesVO currenciesVO, HttpServletRequest request) {
		if (currenciesVO.getCurrencyCode() != null && currenciesRepository.existsById(currenciesVO.getCurrencyCode())) {
			throw new ResourceAlreadyExistsException(CURRENCY, CURR_CODE,
					CURR_WITH_CODE + currenciesVO.getCurrencyCode() + " already exists.");
		}
		try {
			currenciesVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
			CurrenciesEntity entity = mapCurrenciesVOToEntity(currenciesVO);
			currenciesRepository.save(entity);
			return "Saved Successfully";
		} catch (DataIntegrityViolationException ex) {
			log.error("Error saveCurrency() ", ex);
			String constraint = ExceptionUtils.extractConstraintName(ex);
			throw mapConstraintToException(constraint);
		} catch (Exception ex) {
			log.error("Error saveCurrency() ", ex);
			throw new ServiceException("Failed to save", ex);
		}
	}

	public String updateCurrency(CurrenciesVO currenciesVO, String code, HttpServletRequest request) {
		try {
			if (currenciesVO.getCurrencyCode() != null && !code.equalsIgnoreCase(currenciesVO.getCurrencyCode())) {
				throw new IllegalArgumentException("Invalid request. Please verify the submitted details.");
			}
			CurrenciesEntity existingEntity = currenciesRepository.findById(code)
					.orElseThrow(() -> new ResourceNotFoundException(CURRENCY, CURR_CODE,
							CURR_WITH_CODE + code + NOT_FOUND));

			currenciesVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
			updateCurrenciesEntity(existingEntity, currenciesVO);
			currenciesRepository.save(existingEntity);
			return "Updated Successfully";
		} catch (DataIntegrityViolationException ex) {
			log.error("Error updateCurrency() ", ex);
			String constraint = ExceptionUtils.extractConstraintName(ex);
			throw mapConstraintToException(constraint);
		} catch (ResourceNotFoundException | IllegalArgumentException ex) {
			throw ex;
		} catch (Exception ex) {
			log.error("Error updateCurrency() ", ex);
			throw new ServiceException("Failed to update", ex);
		}
	}

	public String deleteCurrency(String code) {
		if (!currenciesRepository.existsById(code)) {
			throw new ResourceNotFoundException(CURRENCY, CURR_CODE, CURR_WITH_CODE + code + NOT_FOUND);
		}
		try {
			currenciesRepository.deleteById(code);
			return "Deleted Successfully";
		} catch (Exception ex) {
			log.error("Error deleteCurrency() ", ex);
			throw new ServiceException("Failed to delete", ex);
		}
	}

	private RuntimeException mapConstraintToException(String constraint) {
		if (constraint == null) {
			return new ResourceAlreadyExistsException(CURRENCY, "unknown", "Error");
		}
		return switch (constraint) {
		case "UK_CUR_ALPHA_CODE" ->
			new ResourceAlreadyExistsException(CURRENCY, "alphaCode", "Alpha code already exists");
		case "UK_CUR_NAME" ->
			new ResourceAlreadyExistsException(CURRENCY, "currencyName", "Currency name already exists");
		case "PK_CUR_CODE" -> new ResourceAlreadyExistsException(CURRENCY, CURR_CODE, "Currency code already exists");
		default -> new ResourceAlreadyExistsException(CURRENCY, "unknown", "Error");
		};
	}

	@Override
	public ResponseEntity<Object> getAllCurrencies() {
		var entities = currenciesRepository.findAll();
		List<CurrenciesVO> voList = entities.stream().map(this::mapCurrenciesEntityToVO).toList();
		return ResponseEntity.ok(voList);
	}
}
