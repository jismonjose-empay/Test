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

import com.empay.parameterservice.entities.DistributorsEntity;
import com.empay.parameterservice.exceptions.ResourceAlreadyExistsException;
import com.empay.parameterservice.exceptions.ResourceNotFoundException;
import com.empay.parameterservice.exceptions.ServiceException;
import com.empay.parameterservice.repository.AggregatorsRepository;
import com.empay.parameterservice.repository.CitiesRepo;
import com.empay.parameterservice.repository.DistributorsRepository;
import com.empay.parameterservice.repository.StatesRepo;
import com.empay.parameterservice.service.DistributorsService;
import com.empay.parameterservice.utils.ExceptionUtils;
import com.empay.parameterservice.utils.JwtUtils;
import com.empay.parameterservice.valueobjects.DistributorsVO;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class DistributorsServiceImpl implements DistributorsService {

	private final DistributorsRepository distributorsRepository;
	private final JwtUtils jwtUtils;
	private final AggregatorsRepository aggregatorsRepository;
	private final CitiesRepo citiesRepo;
	private final StatesRepo statesRepo;

	private static final DateTimeFormatter DateFormatTime = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

	private static final String SERIAL_NUMBER = "serialNumber";
	private static final String DISTRIBUTOR_WITH_ID = "Distributor with id ";
	private static final String NOT_FOUND = " not found.";
	private static final String DISTRIBUTOR = "Distributor";

	public ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search) {
		Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

		Integer defaultPageNumber = Objects.nonNull(page) ? page - 1 : 0;
		Pageable pageable = PageRequest.of(defaultPageNumber, size, sort);
		Page<DistributorsEntity> distributorsPage;
		if (search != null && !search.trim().isEmpty()) {
			Specification<DistributorsEntity> specification = (root, query, cb) -> cb.or(
					cb.like(cb.lower(root.get("dstType")), "%" + search.toLowerCase() + "%"),
					cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"));
			distributorsPage = distributorsRepository.findAll(specification, pageable);
		} else {
			distributorsPage = distributorsRepository.findAll(pageable);
		}
		List<DistributorsVO> voList = distributorsPage.getContent().stream().map(this::mapDistributorsEntityToVO)
				.toList();

		Map<String, Object> responseVO = new HashMap<>();
		responseVO.put("voList", voList);
		responseVO.put("totalElements", distributorsPage.getTotalElements());
		return new ResponseEntity<>(responseVO, HttpStatus.OK);
	}

	public ResponseEntity<Object> findById(Integer id) {
		DistributorsVO distributors = mapDistributorsEntityToVO(distributorsRepository.findById(id).orElseThrow(
				() -> new ResourceNotFoundException(DISTRIBUTOR, SERIAL_NUMBER, DISTRIBUTOR_WITH_ID + id + NOT_FOUND)));

		return ResponseEntity.ok(distributors);
	}

	public DistributorsVO mapDistributorsEntityToVO(DistributorsEntity entity) {
		if (entity == null) {
			return null;
		}
		DistributorsVO vo = new DistributorsVO();
		vo.setSerialNumber(entity.getSerialNumber());
		vo.setLastUpdatedDate(
				entity.getLastUpdatedDate() != null ? entity.getLastUpdatedDate().format(DateFormatTime) : null);
		vo.setUpdatedUser(entity.getUpdatedUser() != null ? entity.getUpdatedUser() : null);
		vo.setInstitutionCode(entity.getInstitutionCode());
		vo.setDstType(entity.getDstType());
		vo.setName(entity.getName());
		vo.setLocation(entity.getLocation());
		vo.setCity(entity.getCity() != null ? entity.getCity().getCityName() : null);
		vo.setCitySerNum(entity.getCity() != null ? entity.getCity().getSerialNumber() : null);
		vo.setState(entity.getState() != null ? entity.getState().getStateName() : null);
		vo.setStateSerNum(entity.getState() != null ? entity.getState().getStateCode() : null);
		vo.setZipCode(entity.getZipCode());
		vo.setContactPerson(entity.getContactPerson());
		vo.setMobile(entity.getMobile());
		vo.setEmail(entity.getEmail());
		vo.setAggregatorSerialNo(entity.getAggregator().getSerialNumber());

		return vo;
	}

	public DistributorsEntity mapDistributorsVOToEntity(DistributorsVO vo) {
		if (vo == null) {
			return null;
		}
		DistributorsEntity entity = new DistributorsEntity();
		entity.setSerialNumber(vo.getSerialNumber());
		entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
		entity.setUpdatedUser(vo.getUpdatedUser());
		entity.setInstitutionCode(vo.getInstitutionCode());
		entity.setDstType(vo.getDstType());
		entity.setName(vo.getName());
		entity.setLocation(vo.getLocation());

		entity.setCity(vo.getCitySerNum() != null ? citiesRepo.findById(vo.getCitySerNum()).orElseThrow(
				() -> new ResourceNotFoundException(DISTRIBUTOR, "city", "City not found: " + vo.getCitySerNum()))
				: null);
		entity.setState(vo.getStateSerNum() != null ? statesRepo.findById(vo.getStateSerNum()).orElseThrow(
				() -> new ResourceNotFoundException(DISTRIBUTOR, "state", "State not found: " + vo.getStateSerNum()))
				: null);
		entity.setZipCode(vo.getZipCode());
		entity.setContactPerson(vo.getContactPerson());
		entity.setMobile(vo.getMobile());
		entity.setEmail(vo.getEmail());
		entity.setAggregator(vo.getAggregatorSerialNo() != null ? aggregatorsRepository
				.findById(vo.getAggregatorSerialNo()).orElseThrow(() -> new ResourceNotFoundException("Distributors",
						"aggregator", "Aggregator not found: " + vo.getAggregatorSerialNo()))
				: null);

		return entity;
	}

	private void updateDistributorsEntity(DistributorsEntity entity, DistributorsVO vo) {

		entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
		entity.setUpdatedUser(vo.getUpdatedUser());
		entity.setInstitutionCode(vo.getInstitutionCode());

		entity.setDstType(vo.getDstType());
		entity.setName(vo.getName());
		entity.setLocation(vo.getLocation());

		entity.setCity(vo.getCitySerNum() != null ? citiesRepo.findById(vo.getCitySerNum()).orElseThrow(
				() -> new ResourceNotFoundException("Distributor", "City", "City not found: " + vo.getCitySerNum()))
				: null);

		entity.setState(vo.getStateSerNum() != null ? statesRepo.findById(vo.getStateSerNum()).orElseThrow(
				() -> new ResourceNotFoundException("Distributor", "State", "State not found: " + vo.getStateSerNum()))
				: null);

		entity.setZipCode(vo.getZipCode());
		entity.setContactPerson(vo.getContactPerson());
		entity.setMobile(vo.getMobile());
		entity.setEmail(vo.getEmail());

		entity.setAggregator(vo.getAggregatorSerialNo() != null ? aggregatorsRepository
				.findById(vo.getAggregatorSerialNo()).orElseThrow(() -> new ResourceNotFoundException("Distributor",
						"Aggregator", "Aggregator not found: " + vo.getAggregatorSerialNo()))
				: null);
	}

	public String saveDistributor(DistributorsVO distributorsVO, HttpServletRequest request) {
		if (distributorsVO.getSerialNumber() != null
				&& distributorsRepository.existsById(distributorsVO.getSerialNumber())) {
			throw new ResourceAlreadyExistsException(DISTRIBUTOR, SERIAL_NUMBER,
					DISTRIBUTOR_WITH_ID + distributorsVO.getSerialNumber() + " already exists");
		}
		try {
			distributorsVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
			distributorsVO.setInstitutionCode(jwtUtils.getInstitutionCode(request));
			DistributorsEntity entity = mapDistributorsVOToEntity(distributorsVO);
			distributorsRepository.saveAndFlush(entity);
			return "Saved Successfully";
		} catch (DataIntegrityViolationException ex) {
			log.error("Error saveDistributor() ", ex);
			String constraint = ExceptionUtils.extractConstraintName(ex);
			throw mapConstraintToException(constraint);
		} catch (Exception ex) {
			log.error("Error saveDistributor() ", ex);
			throw new ServiceException("Failed to save", ex);
		}
	}

	public String updateDistributor(DistributorsVO distributorsVO, Integer id, HttpServletRequest request) {
		try {
			if (distributorsVO.getSerialNumber() != null && !id.equals(distributorsVO.getSerialNumber())) {
				throw new IllegalArgumentException("Invalid request. Please verify the submitted details.");
			}
			DistributorsEntity existingEntity = distributorsRepository.findById(id)
					.orElseThrow(() -> new ResourceNotFoundException(DISTRIBUTOR, SERIAL_NUMBER,
							DISTRIBUTOR_WITH_ID + id + NOT_FOUND));
			distributorsVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
			distributorsVO.setInstitutionCode(jwtUtils.getInstitutionCode(request));
			updateDistributorsEntity(existingEntity, distributorsVO);
			distributorsRepository.saveAndFlush(existingEntity);
			return "Updated Successfully";
		} catch (DataIntegrityViolationException ex) {
			log.error("Error updateDistributor() ", ex);
			String constraint = ExceptionUtils.extractConstraintName(ex);
			throw mapConstraintToException(constraint);

		} catch (ResourceNotFoundException | IllegalArgumentException ex) {
			throw ex;

		} catch (Exception ex) {
			log.error("Error updateDistributor() ", ex);
			throw new ServiceException("Failed to update", ex);
		}
	}

	public String deleteDistributor(Integer id) {
		if (!distributorsRepository.existsById(id)) {
			throw new ResourceNotFoundException(DISTRIBUTOR, SERIAL_NUMBER, DISTRIBUTOR_WITH_ID + id + NOT_FOUND);
		}
		try {
			distributorsRepository.deleteById(id);
			return "Deleted Successfully";
		} catch (Exception ex) {
			log.error("Error deleteDistributor() ", ex);
			throw new ServiceException("Failed to delete", ex);
		}
	}

	private RuntimeException mapConstraintToException(String constraint) {
		if (constraint == null) {
			return new ResourceAlreadyExistsException(DISTRIBUTOR, "unknown", "Error");
		}
		return switch (constraint) {
		case "UK_DST_DISTRIBUTOR_NAME" -> new ResourceAlreadyExistsException(DISTRIBUTOR, "groupName",
				"Distributor name already exists for this aggregator and institution");
		case "PK_DST_SER_NUMBER" -> new ResourceAlreadyExistsException(DISTRIBUTOR, SERIAL_NUMBER,
				"Distributor with serial number already exists");
		default -> new ResourceAlreadyExistsException(DISTRIBUTOR, "unknown", "Error");
		};
	}
}
