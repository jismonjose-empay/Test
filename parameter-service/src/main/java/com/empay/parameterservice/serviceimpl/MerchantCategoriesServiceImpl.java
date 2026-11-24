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

import com.empay.parameterservice.entities.MerchantCategoriesEntity;
import com.empay.parameterservice.exceptions.ResourceAlreadyExistsException;
import com.empay.parameterservice.exceptions.ResourceNotFoundException;
import com.empay.parameterservice.exceptions.ServiceException;
import com.empay.parameterservice.repository.MerchantCategoriesRepository;
import com.empay.parameterservice.service.MerchantCategoriesService;
import com.empay.parameterservice.utils.ExceptionUtils;
import com.empay.parameterservice.utils.JwtUtils;
import com.empay.parameterservice.valueobjects.MerchantCategoriesVO;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class MerchantCategoriesServiceImpl implements MerchantCategoriesService {

	private final MerchantCategoriesRepository merchantCategoriesRepository;
	private final JwtUtils jwtUtils;

	private static final DateTimeFormatter DateFormatTime = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

	private static final String MERCHANT_CATEGORY = "Merchant Category";
	private static final String NOT_FOUND = " not found.";
	private static final String MERCHANT_CATEGORY_WITH_ID = "Merchant category with id ";

	public ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search) {
		Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

		Integer defaultPageNumber = Objects.nonNull(page) ? page - 1 : 0;
		Pageable pageable = PageRequest.of(defaultPageNumber, size, sort);
		Page<MerchantCategoriesEntity> meCategoriesPage;
		if (search != null && !search.trim().isEmpty()) {
			Specification<MerchantCategoriesEntity> specification = (root, query, cb) -> cb.or(
					cb.like(cb.lower(root.get("merchantCategoryCode")), "%" + search.toLowerCase() + "%"),
					cb.like(cb.lower(root.get("merchantCategoryTcc")), "%" + search.toLowerCase() + "%"),
					cb.like(cb.lower(root.get("merchantCategoryDescription")), "%" + search.toLowerCase() + "%"),
					cb.like(cb.lower(root.get("businessSegments")), "%" + search.toLowerCase() + "%"),
					cb.like(cb.lower(root.get("merchantCategoryStatus")), "%" + search.toLowerCase() + "%"));
			meCategoriesPage = merchantCategoriesRepository.findAll(specification, pageable);
		} else {
			meCategoriesPage = merchantCategoriesRepository.findAll(pageable);
		}
		List<MerchantCategoriesVO> voList = meCategoriesPage.getContent().stream()
				.map(this::mapMerchantCategoriesEntityToVO).toList();

		Map<String, Object> responseVO = new HashMap<>();
		responseVO.put("voList", voList);
		responseVO.put("totalElements", meCategoriesPage.getTotalElements());
		return new ResponseEntity<>(responseVO, HttpStatus.OK);
	}

	public ResponseEntity<Object> findById(String id) {
		MerchantCategoriesVO meCategories = mapMerchantCategoriesEntityToVO(merchantCategoriesRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(MERCHANT_CATEGORY, "mcc",
						MERCHANT_CATEGORY_WITH_ID + id + NOT_FOUND)));

		return ResponseEntity.ok(meCategories);
	}

	public MerchantCategoriesVO mapMerchantCategoriesEntityToVO(MerchantCategoriesEntity entity) {
		if (entity == null)
			return null;
		MerchantCategoriesVO vo = new MerchantCategoriesVO();
		vo.setMerchantCategoryCode(entity.getMerchantCategoryCode());
		vo.setLastUpdatedDate(
				entity.getLastUpdatedDate() != null ? entity.getLastUpdatedDate().format(DateFormatTime) : null);
		vo.setUpdatedUser(entity.getUpdatedUser());
		vo.setMerchantCategoryDescription(entity.getMerchantCategoryDescription());
		vo.setMerchantCategoryTcc(entity.getMerchantCategoryTcc());
		vo.setBusinessSegments(entity.getBusinessSegments());
		vo.setPriority(entity.getPriority());
		vo.setMerchantCategoryStatus(entity.getMerchantCategoryStatus());
		return vo;
	}

	public MerchantCategoriesEntity mapMerchantCategoriesVOToEntity(MerchantCategoriesVO vo) {
		if (vo == null) {
			return null;
		}
		MerchantCategoriesEntity entity = new MerchantCategoriesEntity();
		entity.setMerchantCategoryCode(vo.getMerchantCategoryCode());
		entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
		entity.setUpdatedUser(vo.getUpdatedUser());
		entity.setMerchantCategoryDescription(vo.getMerchantCategoryDescription());
		entity.setMerchantCategoryTcc(vo.getMerchantCategoryTcc());
		entity.setBusinessSegments(vo.getBusinessSegments());
		entity.setPriority(vo.getPriority());
		entity.setMerchantCategoryStatus(vo.getMerchantCategoryStatus());
		return entity;
	}

	private void updateMerchantCategoriesEntity(MerchantCategoriesEntity entity, MerchantCategoriesVO vo) {

		entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
		entity.setUpdatedUser(vo.getUpdatedUser());

		entity.setMerchantCategoryDescription(vo.getMerchantCategoryDescription());

		entity.setMerchantCategoryTcc(vo.getMerchantCategoryTcc());
		entity.setBusinessSegments(vo.getBusinessSegments());
		entity.setPriority(vo.getPriority());
		entity.setMerchantCategoryStatus(vo.getMerchantCategoryStatus());
	}

	public String saveMerchantCategory(MerchantCategoriesVO merchantCategoriesVO, HttpServletRequest request) {
		if (merchantCategoriesVO.getMerchantCategoryCode() != null
				&& merchantCategoriesRepository.existsById(merchantCategoriesVO.getMerchantCategoryCode())) {
			throw new ResourceAlreadyExistsException(MERCHANT_CATEGORY, "mcc", "Merchant category with code "
					+ merchantCategoriesVO.getMerchantCategoryCode() + " already exists");
		}
		try {
			merchantCategoriesVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
			MerchantCategoriesEntity entity = mapMerchantCategoriesVOToEntity(merchantCategoriesVO);
			merchantCategoriesRepository.save(entity);
			return "Saved Successfully";
		} catch (DataIntegrityViolationException ex) {
			log.error("Error saveMerchantCategory() ", ex);
			String constraint = ExceptionUtils.extractConstraintName(ex);
			throw mapConstraintToException(constraint);
		} catch (Exception ex) {
			log.error("Error saveMerchantCategory() ", ex);
			throw new ServiceException("Failed to save", ex);
		}
	}

	public String updateMerchantCategory(MerchantCategoriesVO merchantCategoriesVO, String id,
			HttpServletRequest request) {
		try {
			if (merchantCategoriesVO.getMerchantCategoryCode() != null
					&& !id.equalsIgnoreCase(merchantCategoriesVO.getMerchantCategoryCode())) {
				throw new IllegalArgumentException("Invalid request. Please verify the submitted details.");
			}
			MerchantCategoriesEntity existingEntity = merchantCategoriesRepository.findById(id)
					.orElseThrow(() -> new ResourceNotFoundException(MERCHANT_CATEGORY, "MCC",
							MERCHANT_CATEGORY_WITH_ID + id + NOT_FOUND));
			merchantCategoriesVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
			updateMerchantCategoriesEntity(existingEntity, merchantCategoriesVO);
			merchantCategoriesRepository.saveAndFlush(existingEntity);
			return "Updated Successfully";
		} catch (DataIntegrityViolationException ex) {
			log.error("Error updateMerchantCategory() ", ex);
			String constraint = ExceptionUtils.extractConstraintName(ex);
			throw mapConstraintToException(constraint);
		} catch (ResourceNotFoundException | IllegalArgumentException ex) {
			throw ex;
		} catch (Exception ex) {
			log.error("Error updateMerchantCategory() ", ex);
			throw new ServiceException("Failed to update", ex);
		}
	}

	public String deleteMerchantCategory(String id) {
		if (!merchantCategoriesRepository.existsById(id)) {
			throw new ResourceNotFoundException(MERCHANT_CATEGORY, "mcc", MERCHANT_CATEGORY_WITH_ID + id + NOT_FOUND);
		}
		try {
			merchantCategoriesRepository.deleteById(id);
			return "Deleted Successfully";
		} catch (Exception ex) {
			log.error("Error deleteMerchantCategory() ", ex);
			throw new ServiceException("Failed to delete", ex);
		}
	}

	private RuntimeException mapConstraintToException(String constraint) {
		if (constraint == null) {
			return new ResourceAlreadyExistsException(MERCHANT_CATEGORY, "unknown", "Error");
		}
		return switch (constraint) {
		case "UK_MCC_DESCRIPTION" ->
			new ResourceAlreadyExistsException(MERCHANT_CATEGORY, "description", "Description already exists");
		case "PK_MCC_CODE" -> new ResourceAlreadyExistsException(MERCHANT_CATEGORY, "mcc", "MCC already exists");
		default -> new ResourceAlreadyExistsException(MERCHANT_CATEGORY, "unknown", "Error");
		};
	}
}
