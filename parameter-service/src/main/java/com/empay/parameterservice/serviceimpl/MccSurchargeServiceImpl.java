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

import com.empay.parameterservice.entities.MccSurchargeEntity;
import com.empay.parameterservice.exceptions.ResourceAlreadyExistsException;
import com.empay.parameterservice.exceptions.ResourceNotFoundException;
import com.empay.parameterservice.exceptions.ServiceException;
import com.empay.parameterservice.repository.MccSurchargeRepository;
import com.empay.parameterservice.service.MccSurchargeService;
import com.empay.parameterservice.utils.ExceptionUtils;
import com.empay.parameterservice.utils.JwtUtils;
import com.empay.parameterservice.valueobjects.MccSurchargeVO;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class MccSurchargeServiceImpl implements MccSurchargeService {

	private final MccSurchargeRepository mccSurchargeRepository;
	private final JwtUtils jwtUtils;

	private static final String MCC_SURCHARGE = "MCC Surcharge";
	private static final String NOT_FOUND = " not found.";
	private static final String SERIAL_NUMBER = "serialNumber";
	private static final String MCC_SURCHARGE_WITH_ID = "MCC Surcharge with id ";

	private static final DateTimeFormatter DateFormatTime = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

	public ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search) {
		Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

		Integer defaultPageNumber = Objects.nonNull(page) ? page - 1 : 0;
		Pageable pageable = PageRequest.of(defaultPageNumber, size, sort);
		Page<MccSurchargeEntity> mccSurchargePage;
		if (search != null && !search.trim().isEmpty()) {
			Specification<MccSurchargeEntity> specification = (root, query, cb) -> cb.or(
					cb.like(cb.lower(root.get("mccCode")), "%" + search.toLowerCase() + "%"),
					cb.like(cb.lower(root.get("mscIntCategory")), "%" + search.toLowerCase() + "%"),
					cb.like(root.get("toAmount").as(String.class), "%" + search + "%"),
					cb.like(root.get("fromAmount").as(String.class), "%" + search + "%"));
			mccSurchargePage = mccSurchargeRepository.findAll(specification, pageable);
		} else {
			mccSurchargePage = mccSurchargeRepository.findAll(pageable);
		}
		List<MccSurchargeVO> voList = mccSurchargePage.getContent().stream().map(this::mapMccSurchargeEntityToVO)
				.toList();
		Map<String, Object> responseVO = new HashMap<>();
		responseVO.put("voList", voList);
		responseVO.put("totalElements", mccSurchargePage.getTotalElements());
		return new ResponseEntity<>(responseVO, HttpStatus.OK);
	}

	public ResponseEntity<Object> findById(Integer id) {
		MccSurchargeVO mccSurcharge = mapMccSurchargeEntityToVO(
				mccSurchargeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(MCC_SURCHARGE,
						SERIAL_NUMBER, MCC_SURCHARGE_WITH_ID + id + NOT_FOUND)));

		return ResponseEntity.ok(mccSurcharge);
	}

	public MccSurchargeVO mapMccSurchargeEntityToVO(MccSurchargeEntity entity) {
		if (entity == null) {
			return null;
		}
		MccSurchargeVO vo = new MccSurchargeVO();
		vo.setSerialNumber(entity.getSerialNumber());
		vo.setLastUpdatedDate(
				entity.getLastUpdatedDate() != null ? entity.getLastUpdatedDate().format(DateFormatTime) : null);
		vo.setUpdatedUser(entity.getUpdatedUser());
		vo.setInstitutionCode(entity.getInstitutionCode());
		vo.setMscIntCategory(entity.getMscIntCategory());
		vo.setMccCode(entity.getMccCode());
		vo.setCardType(entity.getCardType());
		vo.setTxnMode(entity.getTxnMode());
		vo.setFromAmount(entity.getFromAmount());
		vo.setToAmount(entity.getToAmount());
		vo.setMscPercentage(entity.getMscPercentage());
		vo.setMinAmount(entity.getMinAmount());
		vo.setMaxAmount(entity.getMaxAmount());
		vo.setRemarks(entity.getRemarks());
		return vo;
	}

	public MccSurchargeEntity mapMccSurchargeVOToEntity(MccSurchargeVO vo) {
		if (vo == null) {
			return null;
		}
		MccSurchargeEntity entity = new MccSurchargeEntity();
		entity.setSerialNumber(vo.getSerialNumber());
		entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
		entity.setUpdatedUser(vo.getUpdatedUser());
		entity.setInstitutionCode(vo.getInstitutionCode());
		entity.setMscIntCategory(vo.getMscIntCategory());
		entity.setMccCode(vo.getMccCode());
		entity.setCardType(vo.getCardType());
		entity.setTxnMode(vo.getTxnMode());
		entity.setFromAmount(vo.getFromAmount());
		entity.setToAmount(vo.getToAmount());
		entity.setMscPercentage(vo.getMscPercentage());
		entity.setMinAmount(vo.getMinAmount());
		entity.setMaxAmount(vo.getMaxAmount());
		entity.setRemarks(vo.getRemarks());
		return entity;
	}

	private void updateMccSurchargeEntity(MccSurchargeEntity entity, MccSurchargeVO vo) {

		entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
		entity.setUpdatedUser(vo.getUpdatedUser());

		entity.setMscIntCategory(vo.getMscIntCategory());
		entity.setMccCode(vo.getMccCode());
		entity.setCardType(vo.getCardType());
		entity.setTxnMode(vo.getTxnMode());

		entity.setFromAmount(vo.getFromAmount());
		entity.setToAmount(vo.getToAmount());

		entity.setMscPercentage(vo.getMscPercentage());
		entity.setMinAmount(vo.getMinAmount());
		entity.setMaxAmount(vo.getMaxAmount());

		entity.setRemarks(vo.getRemarks());
	}

	public String saveMccSurcharge(MccSurchargeVO mccSurchargeVO, HttpServletRequest request) {
		if (mccSurchargeVO.getSerialNumber() != null
				&& mccSurchargeRepository.existsById(mccSurchargeVO.getSerialNumber())) {
			throw new ResourceAlreadyExistsException(MCC_SURCHARGE, SERIAL_NUMBER,
					MCC_SURCHARGE_WITH_ID + mccSurchargeVO.getSerialNumber() + " already exists");
		}
		try {
			mccSurchargeVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
			mccSurchargeVO.setInstitutionCode(jwtUtils.getInstitutionCode(request));
			MccSurchargeEntity entity = mapMccSurchargeVOToEntity(mccSurchargeVO);
			mccSurchargeRepository.saveAndFlush(entity);
			return "Saved Successfully";
		} catch (DataIntegrityViolationException ex) {
			log.error("Error saveMccSurcharge() ", ex);
			String constraint = ExceptionUtils.extractConstraintName(ex);
			throw mapConstraintToException(constraint);
		} catch (Exception ex) {
			log.error("Error saveMccSurcharge() ", ex);
			throw new ServiceException("Failed to save", ex);
		}
	}

	public String updateMccSurcharge(MccSurchargeVO mccSurchargeVO, Integer id, HttpServletRequest request) {
		try {
			if (mccSurchargeVO.getSerialNumber() != null && !id.equals(mccSurchargeVO.getSerialNumber())) {
				throw new IllegalArgumentException("Invalid request. Please verify the submitted details.");
			}
			MccSurchargeEntity existingEntity = mccSurchargeRepository.findById(id)
					.orElseThrow(() -> new ResourceNotFoundException(MCC_SURCHARGE, SERIAL_NUMBER,
							MCC_SURCHARGE_WITH_ID + id + NOT_FOUND));
			mccSurchargeVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
			updateMccSurchargeEntity(existingEntity, mccSurchargeVO);
			mccSurchargeRepository.saveAndFlush(existingEntity);
			return "Updated Successfully";
		} catch (DataIntegrityViolationException ex) {
			log.error("Error updateMccSurcharge() ", ex);
			String constraint = ExceptionUtils.extractConstraintName(ex);
			throw mapConstraintToException(constraint);
		} catch (ResourceNotFoundException | IllegalArgumentException ex) {
			throw ex;
		} catch (Exception ex) {
			log.error("Error updateMccSurcharge() ", ex);
			throw new ServiceException("Failed to update", ex);
		}
	}

	public String deleteMccSurcharge(Integer id) {
		if (!mccSurchargeRepository.existsById(id)) {
			throw new ResourceNotFoundException(MCC_SURCHARGE, SERIAL_NUMBER, MCC_SURCHARGE_WITH_ID + id + NOT_FOUND);
		}
		try {
			mccSurchargeRepository.deleteById(id);
			return "Deleted Successfully";
		} catch (Exception ex) {
			log.error("Error deleteMccSurcharge() ", ex);
			throw new ServiceException("Failed to delete", ex);
		}
	}

	private RuntimeException mapConstraintToException(String constraint) {
		if (constraint == null) {
			return new ResourceAlreadyExistsException(MCC_SURCHARGE, "unknown", "Error");
		}
		return switch (constraint) {
		case "UK_MCC_SURCHARGES" -> new ResourceAlreadyExistsException(MCC_SURCHARGE, "mscIntCategory",
				"Category with same MCC already exists for this institution");
		case "PK_MCC_SURCHARGES" -> new ResourceAlreadyExistsException(MCC_SURCHARGE, SERIAL_NUMBER,
				"MCC Surcharge serial number already exists");
		default -> new ResourceAlreadyExistsException(MCC_SURCHARGE, "unknown", "Error");
		};
	}
}
