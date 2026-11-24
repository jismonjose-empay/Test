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

import com.empay.parameterservice.entities.RegionsEntity;
import com.empay.parameterservice.exceptions.ResourceAlreadyExistsException;
import com.empay.parameterservice.exceptions.ResourceNotFoundException;
import com.empay.parameterservice.exceptions.ServiceException;
import com.empay.parameterservice.repository.RegionsRepository;
import com.empay.parameterservice.service.RegionsService;
import com.empay.parameterservice.valueobjects.RegionsVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class RegionsServiceImpl implements RegionsService {

	private final RegionsRepository regionsRepository;

	private static final String NOT_FOUND = " not found.";
	private static final String REGION = "Region";

	private static final Logger logger = LoggerFactory.getLogger(RegionsServiceImpl.class);

	public ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search) {
		Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

		Integer defaultPageNumber = Objects.nonNull(page) ? page - 1 : 0;
		Pageable pageable = PageRequest.of(defaultPageNumber, size, sort);
		Page<RegionsEntity> regionsPage;
		if (search != null && !search.trim().isEmpty()) {
			Specification<RegionsEntity> specification = (root, query, cb) -> cb.or(
					cb.like(cb.lower(root.get("alphaCode")), "%" + search.toLowerCase() + "%"),
					cb.like(cb.lower(root.get("regionName")), "%" + search.toLowerCase() + "%"),
					cb.like(cb.lower(root.get("numericCode")), "%" + search.toLowerCase() + "%"));
			regionsPage = regionsRepository.findAll(specification, pageable);
		} else {
			regionsPage = regionsRepository.findAll(pageable);
		}
		List<RegionsVO> voList = regionsPage.getContent().stream().map(this::mapToRegionsVo).toList();

		Map<String, Object> responseVO = new HashMap<>();
		responseVO.put("voList", voList);
		responseVO.put("totalElements", regionsPage.getTotalElements());
		return new ResponseEntity<>(responseVO, HttpStatus.OK);
	}

	private RegionsVO mapToRegionsVo(RegionsEntity entity) {
		try {
			if (entity == null)
				return null;

			RegionsVO vo = new RegionsVO();
			vo.setSerialNumber(entity.getSerialNumber());
			vo.setLastUpdatedDate(entity.getLastUpdatedDate() != null
					? entity.getLastUpdatedDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
					: null);
			vo.setUpdatedUser(entity.getUpdatedUser());
			vo.setInsCode(entity.getInsCode());
			vo.setRegionalZone(entity.getRegionalZone());
			vo.setAlphaCode(entity.getAlphaCode());
			vo.setNumericCode(entity.getNumericCode());
			vo.setRegionName(entity.getRegionName());
			vo.setStatus(entity.getStatus());

			return vo;
		} catch (Exception e) {
			logger.error("Error : mapToRegionsVo() : ", e);
			return new RegionsVO();
		}
	}

	public ResponseEntity<Object> findById(Integer id) {
		RegionsVO regions = this.mapToRegionsVo(regionsRepository.findById(id).orElseThrow(
				() -> new ResourceNotFoundException(REGION, "serialNumber", "Region with id " + id + NOT_FOUND)));

		return new ResponseEntity<>(regions, HttpStatus.OK);
	}

	public String saveRegion(RegionsVO regionsVO) {
		boolean exists = regionsRepository.existsByRegionNameAndAlphaCodeAndNumericCode(regionsVO.getRegionName(),
				regionsVO.getAlphaCode(), regionsVO.getNumericCode());
		if (exists) {
			throw new ResourceAlreadyExistsException(REGION, REGION,
					"Region already exists with same name,alphacode and numeric code");
		}
		try {
			RegionsEntity entity = this.mapToRegionsEntity(regionsVO);
			regionsRepository.save(Objects.requireNonNull(entity));
			return "Saved Successfully";
		} catch (DataIntegrityViolationException ex) {
			log.error("Error saveRegion() ", ex);
			throw new ResourceAlreadyExistsException(REGION, REGION, "Region already exists");
		} catch (Exception ex) {
			log.error("Error saveRegion() ", ex);
			throw new ServiceException("Failed to save", ex);
		}
	}

	private RegionsEntity mapToRegionsEntity(RegionsVO vo) {
		try {
			if (vo == null)
				return null;

			RegionsEntity entity = new RegionsEntity();
			entity.setSerialNumber(vo.getSerialNumber());
			entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
			entity.setUpdatedUser(vo.getUpdatedUser());
			entity.setInsCode(vo.getInsCode());
			entity.setRegionalZone(vo.getRegionalZone());
			entity.setAlphaCode(vo.getAlphaCode());
			entity.setNumericCode(vo.getNumericCode());
			entity.setRegionName(vo.getRegionName());
			entity.setStatus(vo.getStatus());

			return entity;
		} catch (Exception e) {
			logger.error("Error : mapToRegionsEntity() : ", e);
			return new RegionsEntity();
		}
	}

	public String updateRegion(RegionsVO regionsVO, Integer id) {
		if (regionsVO.getSerialNumber() != null && !id.equals(regionsVO.getSerialNumber())) {
			throw new IllegalArgumentException("Invalid request. Please verify the submitted details.");
		}
		RegionsEntity entity = regionsRepository.findById(id).orElseThrow(
				() -> new ResourceNotFoundException(REGION, "ID", "Region with ID " + id + NOT_FOUND));
		if (regionsRepository.existsByRegionNameAndAlphaCodeAndNumericCodeAndSerialNumberNot(regionsVO.getRegionName(),
				regionsVO.getAlphaCode(), regionsVO.getNumericCode(), id)) {
			throw new ResourceAlreadyExistsException(REGION, "Region", "Region already exists");
		}
		try {
			this.mapUpdateEntity(entity, regionsVO);
			regionsRepository.save(entity);
			return "Updated Successfully";
		} catch (DataIntegrityViolationException ex) {
			log.error("Error updateRegion() ", ex);
			throw new ServiceException("Failed to update", ex);
		} catch (Exception ex) {
			log.error("Error updateRegion() ", ex);
			throw new ServiceException("Failed to update", ex);
		}
	}

	private void mapUpdateEntity(RegionsEntity entity, RegionsVO vo) {
		try {
			if (entity == null || vo == null)
				return;
			entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
			entity.setUpdatedUser(vo.getUpdatedUser());
			entity.setInsCode(vo.getInsCode());
			entity.setRegionalZone(vo.getRegionalZone());
			entity.setAlphaCode(vo.getAlphaCode());
			entity.setNumericCode(vo.getNumericCode());
			entity.setRegionName(vo.getRegionName());
			entity.setStatus(vo.getStatus());
		} catch (Exception e) {
			logger.error("Error: mapUpdateEntity() : ", e);
		}
	}

	public String deleteRegion(Integer id) {
		if (!regionsRepository.existsById(id)) {
			throw new ResourceNotFoundException(REGION, "regions", "Reigion with id " + id + NOT_FOUND);
		}
		try {
			regionsRepository.deleteById(id);
			return "Deleted Successfully";
		} catch (Exception ex) {
			log.error("Error deleteRegion() ", ex);
			throw new ServiceException("Failed to delete", ex);
		}
	}
}
