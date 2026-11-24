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

import com.empay.parameterservice.entities.AggregatorsEntity;
import com.empay.parameterservice.exceptions.ResourceAlreadyExistsException;
import com.empay.parameterservice.exceptions.ResourceNotFoundException;
import com.empay.parameterservice.exceptions.ServiceException;
import com.empay.parameterservice.repository.AggregatorsRepository;
import com.empay.parameterservice.service.AggregatorsService;
import com.empay.parameterservice.utils.ExceptionUtils;
import com.empay.parameterservice.utils.JwtUtils;
import com.empay.parameterservice.valueobjects.AggregatorsVO;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class AggregatorsServiceImpl implements AggregatorsService {

	private final AggregatorsRepository aggregatorsRepository;
	private final JwtUtils jwtUtils;

	private static final DateTimeFormatter DateFormatTime = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

	private static final String AGGREGATOR = "Aggregator";
	private static final String SERIAL_NUMBER = "serialNumber";
	private static final String AGGREGATOR_WITH_ID = "Aggregator with id ";
	private static final String NOT_FOUND = " not found.";

	public ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search) {
		Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

		Integer defaultPageNumber = Objects.nonNull(page) ? page - 1 : 0;
		Pageable pageable = PageRequest.of(defaultPageNumber, size, sort);
		Page<AggregatorsEntity> aggregatorsPage;
		if (search != null && !search.trim().isEmpty()) {
			Specification<AggregatorsEntity> specification = (root, query, cb) -> cb.or(
					cb.like(cb.lower(root.get("code")), "%" + search.toLowerCase() + "%"),
					cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"));
			aggregatorsPage = aggregatorsRepository.findAll(specification, pageable);
		} else {
			aggregatorsPage = aggregatorsRepository.findAll(pageable);
		}
		List<AggregatorsVO> voList = aggregatorsPage.getContent().stream().map(this::mapAggregatorsEntityToVO).toList();
		Map<String, Object> responseVO = new HashMap<>();
		responseVO.put("voList", voList);
		responseVO.put("totalElements", aggregatorsPage.getTotalElements());
		return new ResponseEntity<>(responseVO, HttpStatus.OK);
	}

	public ResponseEntity<Object> findById(Integer id) {
		AggregatorsVO aggregators = mapAggregatorsEntityToVO(aggregatorsRepository.findById(id).orElseThrow(
				() -> new ResourceNotFoundException(AGGREGATOR, SERIAL_NUMBER, AGGREGATOR_WITH_ID + id + NOT_FOUND)));
		return new ResponseEntity<>(aggregators, HttpStatus.OK);
	}

	public AggregatorsVO mapAggregatorsEntityToVO(AggregatorsEntity entity) {
		AggregatorsVO vo = new AggregatorsVO();
		if (entity == null) {
			return vo;
		}
		vo.setSerialNumber(entity.getSerialNumber());
		vo.setLastUpdatedDate(
				entity.getLastUpdatedDate() != null ? entity.getLastUpdatedDate().format(DateFormatTime) : null);
		vo.setUpdatedUser(entity.getUpdatedUser());
		vo.setInstitutionCode(entity.getInstitutionCode());
		vo.setGenStatus(entity.getGenStatus());
		vo.setCode(entity.getCode());
		vo.setName(entity.getName());
		vo.setAddress(entity.getAddress());
		vo.setCity(entity.getCity());
		vo.setState(entity.getState());
		vo.setPinCode(entity.getPinCode());
		vo.setMobile(entity.getMobile());
		vo.setEmail(entity.getEmail());
		vo.setGstin(entity.getGstin());
		vo.setPan(entity.getPan());
		vo.setBankName(entity.getBankName());
		vo.setAggrPriority(entity.getAggrPriority());
		vo.setAggrBatch(entity.getAggrBatch());
		return vo;
	}

	public String saveAggregator(AggregatorsVO aggregatorsVO, HttpServletRequest request) {
		if (aggregatorsVO.getSerialNumber() != null
				&& aggregatorsRepository.existsById(aggregatorsVO.getSerialNumber())) {
			throw new ResourceAlreadyExistsException(AGGREGATOR, SERIAL_NUMBER,
					AGGREGATOR_WITH_ID + aggregatorsVO.getSerialNumber() + " already exists");
		}
		try {
			aggregatorsVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
			AggregatorsEntity entity = mapAggregatorsVOToEntity(aggregatorsVO);
			aggregatorsRepository.saveAndFlush(entity);
			return "Saved Successfully";
		} catch (DataIntegrityViolationException ex) {
			log.error("Error saveAggregator() ", ex);
			String constraint = ExceptionUtils.extractConstraintName(ex);
			throw mapConstraintToException(constraint);
		} catch (Exception ex) {
			log.error("Error saveAggregator() ", ex);
			throw new ServiceException("Failed to save", ex);
		}
	}

	public AggregatorsEntity mapAggregatorsVOToEntity(AggregatorsVO vo) {
		if (vo == null) {
			return null;
		}
		AggregatorsEntity entity = new AggregatorsEntity();
		entity.setSerialNumber(vo.getSerialNumber());
		entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
		entity.setUpdatedUser(vo.getUpdatedUser());
		entity.setInstitutionCode(vo.getInstitutionCode());
		entity.setGenStatus(vo.getGenStatus());
		entity.setCode(vo.getCode());
		entity.setName(vo.getName());
		entity.setAddress(vo.getAddress());
		entity.setCity(vo.getCity());
		entity.setState(vo.getState());
		entity.setPinCode(vo.getPinCode());
		entity.setMobile(vo.getMobile());
		entity.setEmail(vo.getEmail());
		entity.setGstin(vo.getGstin());
		entity.setPan(vo.getPan());
		entity.setBankName(vo.getBankName());
		entity.setAggrPriority(vo.getAggrPriority());
		entity.setAggrBatch(vo.getAggrBatch());
		return entity;
	}

	private void updateAggregatorsEntity(AggregatorsEntity entity, AggregatorsVO vo) {

		entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
		entity.setUpdatedUser(vo.getUpdatedUser());
		entity.setInstitutionCode(vo.getInstitutionCode());
		entity.setGenStatus(vo.getGenStatus());

		// AGR_CODE is updatable=false, so do not modify it.

		entity.setName(vo.getName());
		entity.setAddress(vo.getAddress());
		entity.setCity(vo.getCity());
		entity.setState(vo.getState());
		entity.setPinCode(vo.getPinCode());
		entity.setMobile(vo.getMobile());
		entity.setEmail(vo.getEmail());
		entity.setGstin(vo.getGstin());
		entity.setPan(vo.getPan());
		entity.setBankName(vo.getBankName());
		entity.setAggrPriority(vo.getAggrPriority());
		entity.setAggrBatch(vo.getAggrBatch());
	}

	public String updateAggregator(AggregatorsVO aggregatorsVO, Integer id, HttpServletRequest request) {
		// Validate body ID against URL ID
		if (aggregatorsVO.getSerialNumber() != null && !id.equals(aggregatorsVO.getSerialNumber())) {

			throw new IllegalArgumentException("Invalid request. Please verify the submitted details.");
		}
		// Fetch existing record using URL primary key
		AggregatorsEntity existingEntity = aggregatorsRepository.findById(id).orElseThrow(
				() -> new ResourceNotFoundException(AGGREGATOR, SERIAL_NUMBER, AGGREGATOR_WITH_ID + id + NOT_FOUND));

		try {
			aggregatorsVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
			// Update existing entity instead of creating a new one
			updateAggregatorsEntity(existingEntity, aggregatorsVO);
			aggregatorsRepository.saveAndFlush(existingEntity);
			return "Updated Successfully";

		} catch (DataIntegrityViolationException ex) {

			log.error("Error updateAggregator() ", ex);
			String constraint = ExceptionUtils.extractConstraintName(ex);
			throw mapConstraintToException(constraint);

		} catch (Exception ex) {

			log.error("Error updateAggregator() ", ex);
			throw new ServiceException("Failed to update", ex);
		}
	}

	public String deleteAggregator(Integer id) {
		if (!aggregatorsRepository.existsById(id)) {
			throw new ResourceNotFoundException(AGGREGATOR, SERIAL_NUMBER, AGGREGATOR_WITH_ID + id + NOT_FOUND);
		}
		try {
			aggregatorsRepository.deleteById(id);
			return "Deleted Successfully";
		} catch (Exception ex) {
			log.error("Error deleteAggregator() ", ex);
			throw new ServiceException("Failed to delete", ex);
		}
	}

	private RuntimeException mapConstraintToException(String constraint) {
		if (constraint == null) {
			return new ResourceAlreadyExistsException(AGGREGATOR, "unknown", "Error");
		}
		return switch (constraint) {
		case "UK_AGR_CODE" -> new ResourceAlreadyExistsException(AGGREGATOR, "code",
				"Aggregator code already exists for this institution");
		case "PK_AGR_SER_NUMBER" ->
			new ResourceAlreadyExistsException(AGGREGATOR, SERIAL_NUMBER, "Aggregator serial number already exists");
		default -> new ResourceAlreadyExistsException(AGGREGATOR, "unknown", "Error");
		};
	}
}
