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

import com.empay.parameterservice.entities.CountriesEntity;
import com.empay.parameterservice.exceptions.ResourceAlreadyExistsException;
import com.empay.parameterservice.exceptions.ResourceNotFoundException;
import com.empay.parameterservice.exceptions.ServiceException;
import com.empay.parameterservice.repository.CountriesRepository;
import com.empay.parameterservice.service.CountriesService;
import com.empay.parameterservice.utils.ExceptionUtils;
import com.empay.parameterservice.utils.JwtUtils;
import com.empay.parameterservice.valueobjects.CountriesVO;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class CountriesServiceImpl implements CountriesService {

	private final CountriesRepository countriesRepository;
	private final JwtUtils jwtUtils;

	private static final DateTimeFormatter DateFormatTime = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

	private static final String COUNTRY = "Country";
	private static final String COUNTRY_CODE = "countryCode";
	private static final String CON_WITH_ID = "Country with id ";
	private static final String NOT_FOUND = " not found.";

	public ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search) {
		Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

		Integer defaultPageNumber = Objects.nonNull(page) ? page - 1 : 0;
		Pageable pageable = PageRequest.of(defaultPageNumber, size, sort);
		Page<CountriesEntity> countriesPage;
		if (search != null && !search.trim().isEmpty()) {
			Specification<CountriesEntity> specification = (root, query, cb) -> cb.or(
					cb.like(cb.lower(root.get(COUNTRY_CODE)), "%" + search.toLowerCase() + "%"),
					cb.like(cb.lower(root.get("countryName")), "%" + search.toLowerCase() + "%"),
					cb.like(cb.lower(root.get("alpha3Code")), "%" + search.toLowerCase() + "%"),
					cb.like(cb.lower(root.get("alpha2Code")), "%" + search.toLowerCase() + "%"));
			countriesPage = countriesRepository.findAll(specification, pageable);
		} else {
			countriesPage = countriesRepository.findAll(pageable);
		}
		List<CountriesVO> voList = countriesPage.getContent().stream().map(this::mapCountriesEntityToVO).toList();

		Map<String, Object> responseVO = new HashMap<>();
		responseVO.put("voList", voList);
		responseVO.put("totalElements", countriesPage.getTotalElements());
		return new ResponseEntity<>(responseVO, HttpStatus.OK);
	}

	public ResponseEntity<Object> findById(String id) {
		CountriesVO countries = mapCountriesEntityToVO(countriesRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(COUNTRY, COUNTRY_CODE, CON_WITH_ID + id + NOT_FOUND)));

		return ResponseEntity.ok(countries);
	}

	public CountriesVO mapCountriesEntityToVO(CountriesEntity entity) {
		if (entity == null) {
			return null;
		}
		CountriesVO vo = new CountriesVO();
		vo.setCountryCode(entity.getCountryCode());
		vo.setLastUpdatedDate(
				entity.getLastUpdatedDate() != null ? entity.getLastUpdatedDate().format(DateFormatTime) : null);
		vo.setUpdatedUser(entity.getUpdatedUser());
		vo.setAlpha2Code(entity.getAlpha2Code());
		vo.setAlpha3Code(entity.getAlpha3Code());
		vo.setCountryName(entity.getCountryName());

		return vo;
	}

	public String saveCountry(CountriesVO countriesVO, HttpServletRequest request) {
		if (countriesVO.getCountryCode() != null && countriesRepository.existsById(countriesVO.getCountryCode())) {
			throw new ResourceAlreadyExistsException(COUNTRY, COUNTRY_CODE,
					"Country with code " + countriesVO.getCountryCode() + " already exists");
		}
		try {
			countriesVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
			CountriesEntity entity = mapCountriesVOToEntity(countriesVO);
			countriesRepository.save(entity);
			return "Saved Successfully";
		} catch (DataIntegrityViolationException ex) {
			log.error("Error saveCountry() ", ex);
			String constraint = ExceptionUtils.extractConstraintName(ex);
			throw mapConstraintToException(constraint);
		} catch (Exception ex) {
			log.error("Error saveCountry() ", ex);
			throw new ServiceException("Failed to save", ex);
		}
	}

	public CountriesEntity mapCountriesVOToEntity(CountriesVO vo) {
		if (vo == null) {
			return null;
		}
		CountriesEntity entity = new CountriesEntity();
		entity.setCountryCode(vo.getCountryCode());
		entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
		entity.setUpdatedUser(vo.getUpdatedUser());
		entity.setAlpha2Code(vo.getAlpha2Code());
		entity.setAlpha3Code(vo.getAlpha3Code());
		entity.setCountryName(vo.getCountryName());

		return entity;
	}

	private void updateCountriesEntity(CountriesEntity entity, CountriesVO vo) {

		entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
		entity.setUpdatedUser(vo.getUpdatedUser());
		entity.setAlpha3Code(vo.getAlpha3Code());
		entity.setAlpha2Code(vo.getAlpha2Code());
		entity.setCountryName(vo.getCountryName());
	}

	public String updateCountry(CountriesVO countriesVO, String id, HttpServletRequest request) {

		if (countriesVO.getCountryCode() != null && !id.equalsIgnoreCase(countriesVO.getCountryCode())) {
			throw new IllegalArgumentException("Invalid request. Please verify the submitted details.");
		}
		try {
			CountriesEntity existingEntity = countriesRepository.findById(id)
					.orElseThrow(() -> new ResourceNotFoundException("Country", "Country Code",
							"Country with ID " + id + " not found"));
			countriesVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
			updateCountriesEntity(existingEntity, countriesVO);
			countriesRepository.saveAndFlush(existingEntity);
			return "Updated Successfully";
		} catch (DataIntegrityViolationException ex) {
			log.error("Error updateCountry() ", ex);
			String constraint = ExceptionUtils.extractConstraintName(ex);
			throw mapConstraintToException(constraint);
		} catch (ResourceNotFoundException | IllegalArgumentException ex) {
			throw ex;
		} catch (Exception ex) {
			log.error("Error updateCountry() ", ex);
			throw new ServiceException("Failed to update", ex);
		}
	}

	public String deleteCountry(String id) {
		if (!countriesRepository.existsById(id)) {
			throw new ResourceNotFoundException(COUNTRY, COUNTRY_CODE, CON_WITH_ID + id + NOT_FOUND);
		}
		try {
			countriesRepository.deleteById(id);
			return "Deleted Successfully";
		} catch (Exception ex) {
			log.error("Error deleteCountry() ", ex);
			throw new ServiceException("Failed to delete", ex);
		}
	}

	private RuntimeException mapConstraintToException(String constraint) {
		if (constraint == null) {
			return new ResourceAlreadyExistsException(COUNTRY, "unknown", "Error");
		}
		return switch (constraint) {
		case "UK_CON_ALPHA2_CODE" ->
			new ResourceAlreadyExistsException(COUNTRY, "alpha2Code", "Alpha2 code already exists");
		case "UK_CON_ALPHA3_CODE" ->
			new ResourceAlreadyExistsException(COUNTRY, "alpha3Code", "Alpha3 code already exists");
		case "UK_CON_NAME" -> new ResourceAlreadyExistsException(COUNTRY, "countryName", "Country name already exists");
		case "PK_CON_CODE" -> new ResourceAlreadyExistsException(COUNTRY, COUNTRY_CODE, "Country code already exists");
		default -> new ResourceAlreadyExistsException(COUNTRY, "unknown", "Error");
		};
	}

	@Override
	public ResponseEntity<Object> getAllCountries() {
		var countries = countriesRepository.findAll();
		List<CountriesVO> voList = countries.stream().map(this::mapCountriesEntityToVO).toList();
		return ResponseEntity.ok(voList);
	}
}
