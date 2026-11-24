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

import com.empay.parameterservice.entities.SharingPartnersEntity;
import com.empay.parameterservice.exceptions.ResourceAlreadyExistsException;
import com.empay.parameterservice.exceptions.ResourceNotFoundException;
import com.empay.parameterservice.exceptions.ServiceException;
import com.empay.parameterservice.repository.CitiesRepo;
import com.empay.parameterservice.repository.SharingPartnersRepository;
import com.empay.parameterservice.repository.StatesRepo;
import com.empay.parameterservice.service.SharingPartnersService;
import com.empay.parameterservice.utils.ExceptionUtils;
import com.empay.parameterservice.valueobjects.SharingPartnersVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class SharingPartnersServiceImpl implements SharingPartnersService {

	private static final Logger logger = LoggerFactory.getLogger(SharingPartnersServiceImpl.class);

	private final SharingPartnersRepository sharingPartnersRepository;
	private final CitiesRepo citiesRepo;
	private final StatesRepo statesRepo;

	private static final String SHARING_PARTNER = "Sharing Partner";
	private static final String SERIAL_NUMBER = "serialNumber";
	private static final String SHARING_PARTNER_WITH_ID = "Sharing partner with id ";
	private static final String NOT_FOUND = " not found.";

	public ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search) {
		Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

		Integer defaultPageNumber = Objects.nonNull(page) ? page - 1 : 0;
		Pageable pageable = PageRequest.of(defaultPageNumber, size, sort);
		Page<SharingPartnersEntity> sharingPartnersPage;
		if (search != null && !search.trim().isEmpty()) {
			Specification<SharingPartnersEntity> specification = (root, query, cb) -> cb.or(
					cb.like(cb.lower(root.get("gstin")), "%" + search.toLowerCase() + "%"),
					cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"));
			sharingPartnersPage = sharingPartnersRepository.findAll(specification, pageable);
		} else {
			sharingPartnersPage = sharingPartnersRepository.findAll(pageable);
		}
		List<SharingPartnersVO> voList = sharingPartnersPage.getContent().stream().map(this::mapToSharingPartnersVo)
				.toList();

		Map<String, Object> responseVO = new HashMap<>();
		responseVO.put("voList", voList);
		responseVO.put("totalElements", sharingPartnersPage.getTotalElements());
		return new ResponseEntity<>(responseVO, HttpStatus.OK);
	}

	private SharingPartnersVO mapToSharingPartnersVo(SharingPartnersEntity entity) {
		try {
			if (entity == null)
				return null;

			SharingPartnersVO vo = new SharingPartnersVO();
			vo.setSerialNumber(entity.getSerialNumber());
			vo.setLastUpdatedDate(entity.getLastUpdatedDate() != null
					? entity.getLastUpdatedDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
					: null);
			vo.setUpdatedUser(entity.getUpdatedUser());
			vo.setInstitutionCode(entity.getInstitutionCode());
			vo.setName(entity.getName());
			vo.setAddress(entity.getAddress());
			vo.setCity(entity.getCity() != null ? entity.getCity().getCityName() : null);
			vo.setCitySerNum(entity.getCity() != null ? entity.getCity().getSerialNumber() : null);
			vo.setState(entity.getState() != null ? entity.getState().getStateName() : null);
			vo.setStateSerNum(entity.getState() != null ? entity.getState().getStateCode() : null);
			vo.setPin(entity.getPin());
			vo.setPan(entity.getPan());
			vo.setGstin(entity.getGstin());
			vo.setBankName(entity.getBankName());
			vo.setAccountNumber(entity.getAccountNumber());
			vo.setAccountType(entity.getAccountType());
			vo.setBranch(entity.getBranch());
			vo.setIfscCode(entity.getIfscCode());
			vo.setShareFrequency(entity.getShareFrequency());
			vo.setShareMode(entity.getShareMode());
			vo.setMdrPlan(entity.getMdrPlan());
			vo.setDistributor(entity.getDistributor());
			vo.setAggregatorSrlNo(entity.getAggregatorSrlNo());
			return vo;
		} catch (Exception e) {
			logger.error("Error: mapToSharingPartnersVo() ", e);
			return new SharingPartnersVO();
		}
	}

	public ResponseEntity<Object> findById(Integer id) {
		SharingPartnersVO sharingPartner = this.mapToSharingPartnersVo(
				sharingPartnersRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(SHARING_PARTNER,
						SERIAL_NUMBER, SHARING_PARTNER_WITH_ID + id + NOT_FOUND)));

		return new ResponseEntity<>(sharingPartner, HttpStatus.OK);
	}

	public String saveSharingPartner(SharingPartnersVO sharingPartnersVO) {
		if (sharingPartnersVO.getSerialNumber() != null
				&& sharingPartnersRepository.existsById(sharingPartnersVO.getSerialNumber())) {
			throw new ResourceAlreadyExistsException(SHARING_PARTNER, SERIAL_NUMBER,
					SHARING_PARTNER_WITH_ID + sharingPartnersVO.getSerialNumber() + " already exists");
		}
		try {
			SharingPartnersEntity entity = this.mapToSharingPartnersEntity(sharingPartnersVO);
			sharingPartnersRepository.save(Objects.requireNonNull(entity));
			return "Saved Successfully";
		} catch (DataIntegrityViolationException ex) {
			log.error("Error saveSharingPartner() ", ex);
			String constraint = ExceptionUtils.extractConstraintName(ex);
			throw mapConstraintToException(constraint);
		} catch (Exception ex) {
			log.error("Error saveSharingPartner() ", ex);
			throw new ServiceException("Failed to save", ex);
		}
	}

	private SharingPartnersEntity mapToSharingPartnersEntity(SharingPartnersVO vo) {
		try {
			if (vo == null)
				return null;

			SharingPartnersEntity entity = new SharingPartnersEntity();
			
			entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
			entity.setUpdatedUser(vo.getUpdatedUser());
			entity.setInstitutionCode(vo.getInstitutionCode());
			entity.setName(vo.getName());
			entity.setAddress(vo.getAddress());
			entity.setCity(vo.getCitySerNum() != null ? citiesRepo.findById(vo.getCitySerNum())
					.orElseThrow(() -> new ResourceNotFoundException(SHARING_PARTNER, "city",
							"City not found: " + vo.getCitySerNum()))
					: null);
			entity.setState(vo.getStateSerNum() != null ? statesRepo.findById(vo.getStateSerNum())
					.orElseThrow(() -> new ResourceNotFoundException(SHARING_PARTNER, "state",
							"State not found: " + vo.getStateSerNum()))
					: null);
			entity.setPin(vo.getPin());
			entity.setPan(vo.getPan());
			entity.setGstin(vo.getGstin());
			entity.setBankName(vo.getBankName());
			entity.setAccountNumber(vo.getAccountNumber());
			entity.setAccountType(vo.getAccountType());
			entity.setBranch(vo.getBranch());
			entity.setIfscCode(vo.getIfscCode());
			entity.setShareFrequency(vo.getShareFrequency());
			entity.setShareMode(vo.getShareMode());
			entity.setMdrPlan(vo.getMdrPlan());
			entity.setDistributor(vo.getDistributor());
			entity.setAggregatorSrlNo(vo.getAggregatorSrlNo());
			return entity;
		} catch (Exception e) {
			logger.error("Error: mapToSharingPartnersEntity() ", e);
			return new SharingPartnersEntity();
		}
	}

	public String updateSharingPartner(SharingPartnersVO sharingPartnersVO, Integer id) {

		if (sharingPartnersVO.getSerialNumber() != null && !id.equals(sharingPartnersVO.getSerialNumber())) {
			throw new IllegalArgumentException("Invalid request. Please verify the submitted details.");
		}
		SharingPartnersEntity entity = sharingPartnersRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("SHARING_PARTNER", "SERIAL_NUMBER",
						"SHARING_PARTNER_WITH_ID " + id + " NOT_FOUND"));
		try {
			this.mapUpdateSharingPartners(entity, sharingPartnersVO);
			sharingPartnersRepository.save(entity);
			return "Updated Successfully";
		} catch (DataIntegrityViolationException ex) {
			log.error("Error updateSharingPartner() ", ex);
			String constraint = ExceptionUtils.extractConstraintName(ex);
			throw mapConstraintToException(constraint);
		} catch (Exception ex) {
			log.error("Error updateSharingPartner() ", ex);
			throw new ServiceException("Failed to update", ex);
		}
	}

	private void mapUpdateSharingPartners(SharingPartnersEntity entity, SharingPartnersVO vo) {
		try {
			if (entity == null || vo == null)
				return;

			entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
			entity.setUpdatedUser(vo.getUpdatedUser());
			entity.setInstitutionCode(vo.getInstitutionCode());
			entity.setName(vo.getName());
			entity.setAddress(vo.getAddress());
			entity.setCity(vo.getCitySerNum() != null ? citiesRepo.findById(vo.getCitySerNum())
					.orElseThrow(() -> new ResourceNotFoundException(SHARING_PARTNER, "city",
							"City not found: " + vo.getCitySerNum()))
					: null);
			entity.setState(vo.getStateSerNum() != null ? statesRepo.findById(vo.getStateSerNum())
					.orElseThrow(() -> new ResourceNotFoundException(SHARING_PARTNER, "state",
							"State not found: " + vo.getStateSerNum()))
					: null);
			entity.setPin(vo.getPin());
			entity.setPan(vo.getPan());
			entity.setGstin(vo.getGstin());
			entity.setBankName(vo.getBankName());
			entity.setAccountNumber(vo.getAccountNumber());
			entity.setAccountType(vo.getAccountType());
			entity.setBranch(vo.getBranch());
			entity.setIfscCode(vo.getIfscCode());
			entity.setShareFrequency(vo.getShareFrequency());
			entity.setShareMode(vo.getShareMode());
			entity.setMdrPlan(vo.getMdrPlan());
			entity.setDistributor(vo.getDistributor());
			entity.setAggregatorSrlNo(vo.getAggregatorSrlNo());
		} catch (Exception e) {
			logger.error("Error: mapUpdateSharingPartners() ", e);
		}
	}

	public String deleteSharingPartner(Integer id) {
		if (!sharingPartnersRepository.existsById(id)) {
			throw new ResourceNotFoundException(SHARING_PARTNER, SERIAL_NUMBER,
					SHARING_PARTNER_WITH_ID + id + NOT_FOUND);
		}
		try {
			sharingPartnersRepository.deleteById(id);
			return "Deleted Successfully";
		} catch (Exception ex) {
			log.error("Error deleteSharingPartner() ", ex);
			throw new ServiceException("Failed to delete", ex);
		}
	}

	private RuntimeException mapConstraintToException(String constraint) {
		if (constraint == null) {
			return new ResourceAlreadyExistsException(SHARING_PARTNER, "unknown", "Error");
		}
		return switch (constraint) {
		case "PK_SHP_SER_NUMBER" ->
			new ResourceAlreadyExistsException(SHARING_PARTNER, SERIAL_NUMBER, "Sharing partner already exists");
		default -> new ResourceAlreadyExistsException(SHARING_PARTNER, "unknown", "Error");
		};
	}
}
