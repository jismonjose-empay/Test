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

import com.empay.parameterservice.entities.InstitutionsEntity;
import com.empay.parameterservice.exceptions.ResourceAlreadyExistsException;
import com.empay.parameterservice.exceptions.ResourceNotFoundException;
import com.empay.parameterservice.exceptions.ServiceException;
import com.empay.parameterservice.repository.CountriesRepository;
import com.empay.parameterservice.repository.CurrenciesRepository;
import com.empay.parameterservice.repository.InstitutionsRepository;
import com.empay.parameterservice.service.InstitutionsService;
import com.empay.parameterservice.utils.ExceptionUtils;
import com.empay.parameterservice.utils.JwtUtils;
import com.empay.parameterservice.valueobjects.InstitutionsVO;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class InstitutionsServiceImpl implements InstitutionsService {

	private final CountriesRepository countriesRepository;
	private final CurrenciesRepository currenciesRepository;
	private final InstitutionsRepository institutionsRepository;
	private final JwtUtils jwtUtils;

	private static final String INS_CODE = "institutionCode";
	private static final String INSTITUTION = "Institution";
	private static final String INS_WITH_CODE = "Institution with code ";
	private static final String NOT_FOUND = " not found.";

	public ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search) {
		Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

		Integer defaultPageNumber = Objects.nonNull(page) ? page - 1 : 0;
		Pageable pageable = PageRequest.of(defaultPageNumber, size, sort);
		Page<InstitutionsEntity> institutionsPage;
		if (search != null && !search.trim().isEmpty()) {
			Specification<InstitutionsEntity> specification = (root, query, cb) -> cb.or(
					cb.like(cb.lower(root.get("institutionName")), "%" + search.toLowerCase() + "%"),
					cb.like(cb.lower(root.get("sponsorCode")), "%" + search.toLowerCase() + "%"));
			institutionsPage = institutionsRepository.findAll(specification, pageable);
		} else {
			institutionsPage = institutionsRepository.findAll(pageable);
		}
		List<InstitutionsVO> voList = institutionsPage.getContent().stream().map(this::mapInsEntityToVO).toList();
		Map<String, Object> responseVO = new HashMap<>();
		responseVO.put("voList", voList);
		responseVO.put("totalElements", institutionsPage.getTotalElements());
		return new ResponseEntity<>(responseVO, HttpStatus.OK);
	}

	public ResponseEntity<Object> findById(Integer id) {
		InstitutionsVO institutions = mapInsEntityToVO(institutionsRepository.findById(id).orElseThrow(
				() -> new ResourceNotFoundException(INSTITUTION, INS_CODE, INS_WITH_CODE + id + NOT_FOUND)));

		return ResponseEntity.ok(institutions);
	}

	public InstitutionsVO mapInsEntityToVO(InstitutionsEntity entity) {
		if (entity == null) {
			return null;
		}
		InstitutionsVO vo = new InstitutionsVO();
		vo.setInstitutionCode(entity.getInstitutionCode());
		vo.setLastUpdatedDate(entity.getLastUpdatedDate() != null
				? entity.getLastUpdatedDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
				: null);
		vo.setUpdatedUser(entity.getUpdatedUser());
		vo.setInstitutionName(entity.getInstitutionName());
		vo.setSponsorCode(entity.getSponsorCode());
		vo.setShortName(entity.getShortName());
		vo.setAddress1(entity.getAddress1());
		vo.setAddress2(entity.getAddress2());
		vo.setCity(entity.getCity());
		vo.setState(entity.getState());
		vo.setZipcode(entity.getZipcode());
		vo.setCurrency(entity.getCurrency() != null ? entity.getCurrency().getCurrencyCode() : null);
		vo.setCountry(entity.getCountry() != null ? entity.getCountry().getCountryCode() : null);
		vo.setMakerCheckerFlag(entity.getMakerCheckerFlag());
		vo.setStatusCode(entity.getStatusCode());
		vo.setServiceTax(entity.getServiceTax());
		vo.setSgst(entity.getSgst());
		vo.setCgst(entity.getCgst());
		vo.setCess(entity.getCess());
		vo.setMaxAllowedUser(entity.getMaxAllowedUser());

		return vo;
	}

	public InstitutionsEntity mapInsVOToEntity(InstitutionsVO vo) {
		if (vo == null) {
			return null;
		}

		InstitutionsEntity entity = new InstitutionsEntity();
		entity.setInstitutionCode(vo.getInstitutionCode());
		entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
		entity.setUpdatedUser(vo.getUpdatedUser());
		entity.setInstitutionName(vo.getInstitutionName());
		entity.setSponsorCode(vo.getSponsorCode());
		entity.setShortName(vo.getShortName());
		entity.setAddress1(vo.getAddress1());
		entity.setAddress2(vo.getAddress2());
		entity.setCity(vo.getCity());
		entity.setState(vo.getState());
		entity.setZipcode(vo.getZipcode());
		entity.setCurrency(vo.getCurrency() != null ? currenciesRepository.findById(vo.getCurrency()).orElseThrow(
				() -> new ResourceNotFoundException(INSTITUTION, "currency", "Currency not found: " + vo.getCurrency()))
				: null);
		entity.setCountry(vo.getCountry() != null ? countriesRepository.findById(vo.getCountry()).orElseThrow(
				() -> new ResourceNotFoundException(INSTITUTION, "country", "Country not found: " + vo.getCountry()))
				: null);
		entity.setMakerCheckerFlag(vo.getMakerCheckerFlag());
		entity.setStatusCode(vo.getStatusCode());
		entity.setServiceTax(vo.getServiceTax());
		entity.setSgst(vo.getSgst());
		entity.setCgst(vo.getCgst());
		entity.setCess(vo.getCess());
		entity.setMaxAllowedUser(vo.getMaxAllowedUser());

		return entity;
	}

	private void updateInstitutionEntity(InstitutionsEntity entity, InstitutionsVO vo) {

		entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
		entity.setUpdatedUser(vo.getUpdatedUser());

		entity.setInstitutionName(vo.getInstitutionName());
		entity.setSponsorCode(vo.getSponsorCode());
		entity.setShortName(vo.getShortName());

		entity.setAddress1(vo.getAddress1());
		entity.setAddress2(vo.getAddress2());
		entity.setCity(vo.getCity());
		entity.setState(vo.getState());
		entity.setZipcode(vo.getZipcode());

		entity.setCurrency(vo.getCurrency() != null ? currenciesRepository.findById(vo.getCurrency())
				.orElseThrow(() -> new ResourceNotFoundException("Institution", "Currency",
						"Currency not found: " + vo.getCurrency()))
				: null);

		entity.setCountry(vo.getCountry() != null ? countriesRepository.findById(vo.getCountry()).orElseThrow(
				() -> new ResourceNotFoundException("Institution", "Country", "Country not found: " + vo.getCountry()))
				: null);

		entity.setMakerCheckerFlag(vo.getMakerCheckerFlag());
		entity.setStatusCode(vo.getStatusCode());

		entity.setServiceTax(vo.getServiceTax());
		entity.setSgst(vo.getSgst());
		entity.setCgst(vo.getCgst());
		entity.setCess(vo.getCess());

		entity.setMaxAllowedUser(vo.getMaxAllowedUser());
	}

	public String saveInstitution(InstitutionsVO institutionsVO, HttpServletRequest request) {
		if (institutionsVO.getInstitutionCode() != null
				&& institutionsRepository.existsById(institutionsVO.getInstitutionCode())) {
			throw new ResourceAlreadyExistsException(INSTITUTION, INS_CODE,
					INS_WITH_CODE + institutionsVO.getInstitutionCode() + " already exists");
		}
		try {
			institutionsVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
			institutionsVO.setStatusCode(1);
			InstitutionsEntity entity = mapInsVOToEntity(institutionsVO);
			institutionsRepository.save(entity);
			return "Saved Successfully";
		} catch (DataIntegrityViolationException ex) {
			log.error("Error saveInstitution() ", ex);
			String constraint = ExceptionUtils.extractConstraintName(ex);
			throw mapConstraintToException(constraint);
		} catch (Exception ex) {
			log.error("Error saveInstitution() ", ex);
			throw new ServiceException("Failed to save", ex);
		}
	}

	public String updateInstitution(InstitutionsVO institutionsVO, Integer id, HttpServletRequest request) {
		try {
			if (institutionsVO.getInstitutionCode() != null && !id.equals(institutionsVO.getInstitutionCode())) {
				throw new IllegalArgumentException("Invalid request. Please verify the submitted details.");
			}
			InstitutionsEntity existingEntity = institutionsRepository.findById(id)
					.orElseThrow(() -> new ResourceNotFoundException(INSTITUTION, INS_CODE,
							INS_WITH_CODE + id + NOT_FOUND));
			institutionsVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
			institutionsVO.setStatusCode(1);
			updateInstitutionEntity(existingEntity, institutionsVO);
			institutionsRepository.save(existingEntity);
			return "Updated Successfully";
		} catch (DataIntegrityViolationException ex) {
			log.error("Error updateInstitution() ", ex);
			String constraint = ExceptionUtils.extractConstraintName(ex);
			throw mapConstraintToException(constraint);
		} catch (ResourceNotFoundException | IllegalArgumentException ex) {
			throw ex;
		} catch (Exception ex) {
			log.error("Error updateInstitution() ", ex);
			throw new ServiceException("Failed to update", ex);
		}
	}

	public String deleteInstitution(Integer id) {
		if (!institutionsRepository.existsById(id)) {
			throw new ResourceNotFoundException(INSTITUTION, INS_CODE, INS_WITH_CODE + id + NOT_FOUND);
		}
		try {
			institutionsRepository.deleteById(id);
			return "Deleted Successfully";
		} catch (Exception ex) {
			log.error("Error deleteInstitution() ", ex);
			throw new ServiceException("Failed to delete", ex);
		}
	}

	private RuntimeException mapConstraintToException(String constraint) {
		if (constraint == null) {
			return new ResourceAlreadyExistsException(INSTITUTION, "unknown", "Error");
		}
		return switch (constraint) {
		case "UK_INS_SPS_CODE" ->
			new ResourceAlreadyExistsException(INSTITUTION, "sponsorCode", "Sponsor code already exists");
		case "UK_INS_SHORT_NAME" ->
			new ResourceAlreadyExistsException(INSTITUTION, "shortName", "Short name already exists");
		case "UK_INS_NAME" ->
			new ResourceAlreadyExistsException(INSTITUTION, "institutionName", "Institution name already exists");
		case "PK_INS_CODE" ->
			new ResourceAlreadyExistsException(INSTITUTION, INS_CODE, "Institution code already exists");
		default -> new ResourceAlreadyExistsException(INSTITUTION, "unknown", "Error");
		};
	}
}
