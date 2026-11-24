
package com.empay.parameterservice.serviceimpl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.empay.parameterservice.valueobjects.MDRPlanVO;
import com.empay.parameterservice.entities.MDRPlansEntity;
import com.empay.parameterservice.repository.MDRPlanRepository;
import com.empay.parameterservice.service.MDRPlanService;
import com.empay.parameterservice.utils.JwtUtils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MDRPlanServiceImpl implements MDRPlanService {

	private final MDRPlanRepository mdrPlanRepository;
	private final JwtUtils jwtUtils;

	private MDRPlansEntity convertToEntity(MDRPlanVO dto) {

		MDRPlansEntity entity = new MDRPlansEntity();

		entity.setMdrLastUpdated(dto.getMdrLastUpdated());
		entity.setMdrInsCode(dto.getMdrInsCode());
		entity.setMdrGenStatus(dto.getMdrGenStatus());
		entity.setMdrStatus(dto.getMdrStatus());

		entity.setMdrPlanDescription(dto.getMdrPlanDescription());
		entity.setMdrType(dto.getMdrType());

		// Domestic Debit Slab 1
		entity.setMdrDdSlab1Fixed(dto.getMdrDdSlab1Fixed());
		entity.setMdrDdSlab1Percent(dto.getMdrDdSlab1Percent());
		entity.setMdrDdSlab1Minimum(dto.getMdrDdSlab1Minimum());

		// Domestic Debit Slab 2
		entity.setMdrDdSlab2Fixed(dto.getMdrDdSlab2Fixed());
		entity.setMdrDdSlab2Percent(dto.getMdrDdSlab2Percent());
		entity.setMdrDdSlab2Minimum(dto.getMdrDdSlab2Minimum());

		// Domestic Debit Slab 3
		entity.setMdrDdSlab3Fixed(dto.getMdrDdSlab3Fixed());
		entity.setMdrDdSlab3Percent(dto.getMdrDdSlab3Percent());
		entity.setMdrDdSlab3Minimum(dto.getMdrDdSlab3Minimum());

		// Domestic Credit Slab 1
		entity.setMdrDcSlab1Fixed(dto.getMdrDcSlab1Fixed());
		entity.setMdrDcSlab1Percent(dto.getMdrDcSlab1Percent());
		entity.setMdrDcSlab1Minimum(dto.getMdrDcSlab1Minimum());

		// Domestic Credit Slab 2
		entity.setMdrDcSlab2Fixed(dto.getMdrDcSlab2Fixed());
		entity.setMdrDcSlab2Percent(dto.getMdrDcSlab2Percent());
		entity.setMdrDcSlab2Minimum(dto.getMdrDcSlab2Minimum());

		// Domestic Credit Slab 3
		entity.setMdrDcSlab3Fixed(dto.getMdrDcSlab3Fixed());
		entity.setMdrDcSlab3Percent(dto.getMdrDcSlab3Percent());
		entity.setMdrDcSlab3Minimum(dto.getMdrDcSlab3Minimum());

		// Domestic Credit Slab 4
		entity.setMdrDcSlab4Fixed(dto.getMdrDcSlab4Fixed());
		entity.setMdrDcSlab4Percent(dto.getMdrDcSlab4Percent());
		entity.setMdrDcSlab4Minimum(dto.getMdrDcSlab4Minimum());

		// International Debit
		entity.setMdrIntlDcFixed(dto.getMdrIntlDcFixed());
		entity.setMdrIntlDcPercent(dto.getMdrIntlDcPercent());
		entity.setMdrIntlDcMinimum(dto.getMdrIntlDcMinimum());

		// International Credit
		entity.setMdrIntlCcFixed(dto.getMdrIntlCcFixed());
		entity.setMdrIntlCcPercent(dto.getMdrIntlCcPercent());
		entity.setMdrIntlCcMinimum(dto.getMdrIntlCcMinimum());

		// Diners
		entity.setMdrDinersFixed(dto.getMdrDinersFixed());
		entity.setMdrDinersPercent(dto.getMdrDinersPercent());
		entity.setMdrDinersMinimum(dto.getMdrDinersMinimum());

		// Masterpass
		entity.setMdrMpassFixed(dto.getMdrMpassFixed());
		entity.setMdrMpassPercent(dto.getMdrMpassPercent());
		entity.setMdrMpassMinimum(dto.getMdrMpassMinimum());

		// Cash at POS
		entity.setMdrCashAtPosFixed(dto.getMdrCashAtPosFixed());
		entity.setMdrCashAtPosPercent(dto.getMdrCashAtPosPercent());
		entity.setMdrCashAtPosMinimum(dto.getMdrCashAtPosMinimum());

		// ONUS
		entity.setMdrOnusFixed(dto.getMdrOnusFixed());
		entity.setMdrOnusPercent(dto.getMdrOnusPercent());
		entity.setMdrOnusMinimum(dto.getMdrOnusMinimum());

		// Payment Gateway
		entity.setMdrPaygatewayTransFixed(dto.getMdrPaygatewayTransFixed());
		entity.setMdrPaygatewayTransPercent(dto.getMdrPaygatewayTransPercent());
		entity.setMdrPaygatewayTransMinimum(dto.getMdrPaygatewayTransMinimum());

		// Merchant Category and Aggregator
		entity.setMdrMeCategoryType(dto.getMdrMeCategoryType());
		entity.setMdrAgrSerNumber(dto.getMdrAgrSerNumber());

		// E-wallet
		entity.setMdrEwalletFixed(dto.getMdrEwalletFixed());
		entity.setMdrEwalletPercent(dto.getMdrEwalletPercent());
		entity.setMdrEwalletMinimum(dto.getMdrEwalletMinimum());

		// Business Segment
		entity.setMdrBussSegments(dto.getMdrBussSegments());

		// UPI Upto
		entity.setMdrUpiUptoFixed(dto.getMdrUpiUptoFixed());
		entity.setMdrUpiUptoPercent(dto.getMdrUpiUptoPercent());
		entity.setMdrUpiUptoMinimum(dto.getMdrUpiUptoMinimum());

		// UPI Above
		entity.setMdrUpiAboveFixed(dto.getMdrUpiAboveFixed());
		entity.setMdrUpiAbovePercent(dto.getMdrUpiAbovePercent());
		entity.setMdrUpiAboveMinimum(dto.getMdrUpiAboveMinimum());

		// UPI Prepaid Credit Upto
		entity.setMdrUpiPrepaidCreditUptoFixed(dto.getMdrUpiPrepaidCreditUptoFixed());
		entity.setMdrUpiPrepaidCreditUptoPercent(dto.getMdrUpiPrepaidCreditUptoPercent());
		entity.setMdrUpiPrepaidCreditUptoMinimum(dto.getMdrUpiPrepaidCreditUptoMinimum());

		// UPI Prepaid Credit Above
		entity.setMdrUpiPrepaidCreditAboveFixed(dto.getMdrUpiPrepaidCreditAboveFixed());
		entity.setMdrUpiPrepaidCreditAbovePercent(dto.getMdrUpiPrepaidCreditAbovePercent());
		entity.setMdrUpiPrepaidCreditAboveMinimum(dto.getMdrUpiPrepaidCreditAboveMinimum());

		// Additional Fields
		entity.setMdrLeadSource(dto.getMdrLeadSource());
		entity.setMdrDeviceId(dto.getMdrDeviceId());
		entity.setMdrMerCatId(dto.getMdrMerCatId());

		// International AMEX
		entity.setMdrIntlAmexFixed(dto.getMdrIntlAmexFixed());
		entity.setMdrIntlAmexPercent(dto.getMdrIntlAmexPercent());
		entity.setMdrIntlAmexMinimum(dto.getMdrIntlAmexMinimum());

		// Domestic AMEX
		entity.setMdrDomAmexFixed(dto.getMdrDomAmexFixed());
		entity.setMdrDomAmexPercent(dto.getMdrDomAmexPercent());
		entity.setMdrDomAmexMinimum(dto.getMdrDomAmexMinimum());

		return entity;
	}

	private MDRPlanVO convertToDTO(MDRPlansEntity entity) {

		MDRPlanVO dto = new MDRPlanVO();

		dto.setMdrSerNumber(entity.getMdrSerNumber());
		dto.setMdrLastUpdated(entity.getMdrLastUpdated());
		dto.setMdrUpdatedUser(entity.getMdrUpdatedUser());
		dto.setMdrInsCode(entity.getMdrInsCode());
		dto.setMdrGenStatus(entity.getMdrGenStatus());
		dto.setMdrStatus(entity.getMdrStatus());

		dto.setMdrPlanDescription(entity.getMdrPlanDescription());
		dto.setMdrType(entity.getMdrType());

		// Domestic Debit Slab 1
		dto.setMdrDdSlab1Fixed(entity.getMdrDdSlab1Fixed());
		dto.setMdrDdSlab1Percent(entity.getMdrDdSlab1Percent());
		dto.setMdrDdSlab1Minimum(entity.getMdrDdSlab1Minimum());

		// Domestic Debit Slab 2
		dto.setMdrDdSlab2Fixed(entity.getMdrDdSlab2Fixed());
		dto.setMdrDdSlab2Percent(entity.getMdrDdSlab2Percent());
		dto.setMdrDdSlab2Minimum(entity.getMdrDdSlab2Minimum());

		// Domestic Debit Slab 3
		dto.setMdrDdSlab3Fixed(entity.getMdrDdSlab3Fixed());
		dto.setMdrDdSlab3Percent(entity.getMdrDdSlab3Percent());
		dto.setMdrDdSlab3Minimum(entity.getMdrDdSlab3Minimum());

		// Domestic Credit Slab 1
		dto.setMdrDcSlab1Fixed(entity.getMdrDcSlab1Fixed());
		dto.setMdrDcSlab1Percent(entity.getMdrDcSlab1Percent());
		dto.setMdrDcSlab1Minimum(entity.getMdrDcSlab1Minimum());

		// Domestic Credit Slab 2
		dto.setMdrDcSlab2Fixed(entity.getMdrDcSlab2Fixed());
		dto.setMdrDcSlab2Percent(entity.getMdrDcSlab2Percent());
		dto.setMdrDcSlab2Minimum(entity.getMdrDcSlab2Minimum());

		// Domestic Credit Slab 3
		dto.setMdrDcSlab3Fixed(entity.getMdrDcSlab3Fixed());
		dto.setMdrDcSlab3Percent(entity.getMdrDcSlab3Percent());
		dto.setMdrDcSlab3Minimum(entity.getMdrDcSlab3Minimum());

		// Domestic Credit Slab 4
		dto.setMdrDcSlab4Fixed(entity.getMdrDcSlab4Fixed());
		dto.setMdrDcSlab4Percent(entity.getMdrDcSlab4Percent());
		dto.setMdrDcSlab4Minimum(entity.getMdrDcSlab4Minimum());

		// International Debit
		dto.setMdrIntlDcFixed(entity.getMdrIntlDcFixed());
		dto.setMdrIntlDcPercent(entity.getMdrIntlDcPercent());
		dto.setMdrIntlDcMinimum(entity.getMdrIntlDcMinimum());

		// International Credit
		dto.setMdrIntlCcFixed(entity.getMdrIntlCcFixed());
		dto.setMdrIntlCcPercent(entity.getMdrIntlCcPercent());
		dto.setMdrIntlCcMinimum(entity.getMdrIntlCcMinimum());

		// Diners
		dto.setMdrDinersFixed(entity.getMdrDinersFixed());
		dto.setMdrDinersPercent(entity.getMdrDinersPercent());
		dto.setMdrDinersMinimum(entity.getMdrDinersMinimum());

		// Masterpass
		dto.setMdrMpassFixed(entity.getMdrMpassFixed());
		dto.setMdrMpassPercent(entity.getMdrMpassPercent());
		dto.setMdrMpassMinimum(entity.getMdrMpassMinimum());

		// Cash at POS
		dto.setMdrCashAtPosFixed(entity.getMdrCashAtPosFixed());
		dto.setMdrCashAtPosPercent(entity.getMdrCashAtPosPercent());
		dto.setMdrCashAtPosMinimum(entity.getMdrCashAtPosMinimum());

		// ONUS
		dto.setMdrOnusFixed(entity.getMdrOnusFixed());
		dto.setMdrOnusPercent(entity.getMdrOnusPercent());
		dto.setMdrOnusMinimum(entity.getMdrOnusMinimum());

		// Payment Gateway
		dto.setMdrPaygatewayTransFixed(entity.getMdrPaygatewayTransFixed());
		dto.setMdrPaygatewayTransPercent(entity.getMdrPaygatewayTransPercent());
		dto.setMdrPaygatewayTransMinimum(entity.getMdrPaygatewayTransMinimum());

		// Merchant Category and Aggregator
		dto.setMdrMeCategoryType(entity.getMdrMeCategoryType());
		dto.setMdrAgrSerNumber(entity.getMdrAgrSerNumber());

		// E-wallet
		dto.setMdrEwalletFixed(entity.getMdrEwalletFixed());
		dto.setMdrEwalletPercent(entity.getMdrEwalletPercent());
		dto.setMdrEwalletMinimum(entity.getMdrEwalletMinimum());

		// Business Segment
		dto.setMdrBussSegments(entity.getMdrBussSegments());

		// UPI Upto
		dto.setMdrUpiUptoFixed(entity.getMdrUpiUptoFixed());
		dto.setMdrUpiUptoPercent(entity.getMdrUpiUptoPercent());
		dto.setMdrUpiUptoMinimum(entity.getMdrUpiUptoMinimum());

		// UPI Above
		dto.setMdrUpiAboveFixed(entity.getMdrUpiAboveFixed());
		dto.setMdrUpiAbovePercent(entity.getMdrUpiAbovePercent());
		dto.setMdrUpiAboveMinimum(entity.getMdrUpiAboveMinimum());

		// UPI Prepaid Credit Upto
		dto.setMdrUpiPrepaidCreditUptoFixed(entity.getMdrUpiPrepaidCreditUptoFixed());
		dto.setMdrUpiPrepaidCreditUptoPercent(entity.getMdrUpiPrepaidCreditUptoPercent());
		dto.setMdrUpiPrepaidCreditUptoMinimum(entity.getMdrUpiPrepaidCreditUptoMinimum());

		// UPI Prepaid Credit Above
		dto.setMdrUpiPrepaidCreditAboveFixed(entity.getMdrUpiPrepaidCreditAboveFixed());
		dto.setMdrUpiPrepaidCreditAbovePercent(entity.getMdrUpiPrepaidCreditAbovePercent());
		dto.setMdrUpiPrepaidCreditAboveMinimum(entity.getMdrUpiPrepaidCreditAboveMinimum());

		// Additional Fields
		dto.setMdrLeadSource(entity.getMdrLeadSource());
		dto.setMdrDeviceId(entity.getMdrDeviceId());
		dto.setMdrMerCatId(entity.getMdrMerCatId());

		// International AMEX
		dto.setMdrIntlAmexFixed(entity.getMdrIntlAmexFixed());
		dto.setMdrIntlAmexPercent(entity.getMdrIntlAmexPercent());
		dto.setMdrIntlAmexMinimum(entity.getMdrIntlAmexMinimum());

		// Domestic AMEX
		dto.setMdrDomAmexFixed(entity.getMdrDomAmexFixed());
		dto.setMdrDomAmexPercent(entity.getMdrDomAmexPercent());
		dto.setMdrDomAmexMinimum(entity.getMdrDomAmexMinimum());

		return dto;
	}

	@Override
	public String createMDRPlan(MDRPlanVO mdrPlanVo, HttpServletRequest request) {

		log.info("Creating new MDR Plan");

		MDRPlansEntity mdrPlan = convertToEntity(mdrPlanVo);

		mdrPlan.setMdrLastUpdated(LocalDateTime.now());
		mdrPlan.setMdrUpdatedUser(jwtUtils.getUserSerNumber(request));

		MDRPlansEntity savedPlan = mdrPlanRepository.save(mdrPlan);

		log.info("MDR Plan created successfully. ID: {}", savedPlan.getMdrSerNumber());

		return "MDR Plan created successfully. ID: " + savedPlan.getMdrSerNumber();
	}

	@Override
	@Transactional
	public String updateMDRPlan(MDRPlanVO mdrPlanVo, Integer id, HttpServletRequest request) {

	    if (mdrPlanVo.getMdrSerNumber() != null
	            && !mdrPlanVo.getMdrSerNumber().equals(id)) {
	        throw new IllegalArgumentException(
	                "Invalid request. Please verify the submitted details.");
	    }

	    MDRPlansEntity existingPlan = mdrPlanRepository.findByMdrSerNumber(id)
	            .orElseThrow(() -> new ResponseStatusException(
	                    HttpStatus.NOT_FOUND,
	                    "MDR Plan not found with ID: " + id));
	    // Update the existing entity using the submitted VO
	    updateEntityFromVO(mdrPlanVo, existingPlan);
	    existingPlan.setMdrLastUpdated(LocalDateTime.now());
	    existingPlan.setMdrUpdatedUser(jwtUtils.getUserSerNumber(request));
	    MDRPlansEntity savedPlan = mdrPlanRepository.save(existingPlan);
	    log.info("MDR Plan updated successfully. ID: {}", id);
	    return "MDR Plan updated successfully. ID: " + savedPlan.getMdrSerNumber();
	}
	
	private void updateEntityFromVO(MDRPlanVO dto, MDRPlansEntity entity) {

	    entity.setMdrPlanDescription(dto.getMdrPlanDescription());
	    entity.setMdrType(dto.getMdrType());

	    // Domestic Debit Slab 1
	    entity.setMdrDdSlab1Fixed(dto.getMdrDdSlab1Fixed());
	    entity.setMdrDdSlab1Percent(dto.getMdrDdSlab1Percent());
	    entity.setMdrDdSlab1Minimum(dto.getMdrDdSlab1Minimum());
	    entity.setMdrDdSlab1Fixed(dto.getMdrDdSlab1Fixed());
		entity.setMdrDdSlab1Percent(dto.getMdrDdSlab1Percent());
		entity.setMdrDdSlab1Minimum(dto.getMdrDdSlab1Minimum());

		// Domestic Debit Slab 2
		entity.setMdrDdSlab2Fixed(dto.getMdrDdSlab2Fixed());
		entity.setMdrDdSlab2Percent(dto.getMdrDdSlab2Percent());
		entity.setMdrDdSlab2Minimum(dto.getMdrDdSlab2Minimum());

		// Domestic Debit Slab 3
		entity.setMdrDdSlab3Fixed(dto.getMdrDdSlab3Fixed());
		entity.setMdrDdSlab3Percent(dto.getMdrDdSlab3Percent());
		entity.setMdrDdSlab3Minimum(dto.getMdrDdSlab3Minimum());

		// Domestic Credit Slab 1
		entity.setMdrDcSlab1Fixed(dto.getMdrDcSlab1Fixed());
		entity.setMdrDcSlab1Percent(dto.getMdrDcSlab1Percent());
		entity.setMdrDcSlab1Minimum(dto.getMdrDcSlab1Minimum());

		// Domestic Credit Slab 2
		entity.setMdrDcSlab2Fixed(dto.getMdrDcSlab2Fixed());
		entity.setMdrDcSlab2Percent(dto.getMdrDcSlab2Percent());
		entity.setMdrDcSlab2Minimum(dto.getMdrDcSlab2Minimum());

		// Domestic Credit Slab 3
		entity.setMdrDcSlab3Fixed(dto.getMdrDcSlab3Fixed());
		entity.setMdrDcSlab3Percent(dto.getMdrDcSlab3Percent());
		entity.setMdrDcSlab3Minimum(dto.getMdrDcSlab3Minimum());

		// Domestic Credit Slab 4
		entity.setMdrDcSlab4Fixed(dto.getMdrDcSlab4Fixed());
		entity.setMdrDcSlab4Percent(dto.getMdrDcSlab4Percent());
		entity.setMdrDcSlab4Minimum(dto.getMdrDcSlab4Minimum());

		// International Debit
		entity.setMdrIntlDcFixed(dto.getMdrIntlDcFixed());
		entity.setMdrIntlDcPercent(dto.getMdrIntlDcPercent());
		entity.setMdrIntlDcMinimum(dto.getMdrIntlDcMinimum());

		// International Credit
		entity.setMdrIntlCcFixed(dto.getMdrIntlCcFixed());
		entity.setMdrIntlCcPercent(dto.getMdrIntlCcPercent());
		entity.setMdrIntlCcMinimum(dto.getMdrIntlCcMinimum());

		// Diners
		entity.setMdrDinersFixed(dto.getMdrDinersFixed());
		entity.setMdrDinersPercent(dto.getMdrDinersPercent());
		entity.setMdrDinersMinimum(dto.getMdrDinersMinimum());

		// Masterpass
		entity.setMdrMpassFixed(dto.getMdrMpassFixed());
		entity.setMdrMpassPercent(dto.getMdrMpassPercent());
		entity.setMdrMpassMinimum(dto.getMdrMpassMinimum());

		// Cash at POS
		entity.setMdrCashAtPosFixed(dto.getMdrCashAtPosFixed());
		entity.setMdrCashAtPosPercent(dto.getMdrCashAtPosPercent());
		entity.setMdrCashAtPosMinimum(dto.getMdrCashAtPosMinimum());

		// ONUS
		entity.setMdrOnusFixed(dto.getMdrOnusFixed());
		entity.setMdrOnusPercent(dto.getMdrOnusPercent());
		entity.setMdrOnusMinimum(dto.getMdrOnusMinimum());

		// Payment Gateway
		entity.setMdrPaygatewayTransFixed(dto.getMdrPaygatewayTransFixed());
		entity.setMdrPaygatewayTransPercent(dto.getMdrPaygatewayTransPercent());
		entity.setMdrPaygatewayTransMinimum(dto.getMdrPaygatewayTransMinimum());

		// Merchant Category and Aggregator
		entity.setMdrMeCategoryType(dto.getMdrMeCategoryType());
		entity.setMdrAgrSerNumber(dto.getMdrAgrSerNumber());

		// E-wallet
		entity.setMdrEwalletFixed(dto.getMdrEwalletFixed());
		entity.setMdrEwalletPercent(dto.getMdrEwalletPercent());
		entity.setMdrEwalletMinimum(dto.getMdrEwalletMinimum());

		// Business Segment
		entity.setMdrBussSegments(dto.getMdrBussSegments());

		// UPI Upto
		entity.setMdrUpiUptoFixed(dto.getMdrUpiUptoFixed());
		entity.setMdrUpiUptoPercent(dto.getMdrUpiUptoPercent());
		entity.setMdrUpiUptoMinimum(dto.getMdrUpiUptoMinimum());

		// UPI Above
		entity.setMdrUpiAboveFixed(dto.getMdrUpiAboveFixed());
		entity.setMdrUpiAbovePercent(dto.getMdrUpiAbovePercent());
		entity.setMdrUpiAboveMinimum(dto.getMdrUpiAboveMinimum());

		// UPI Prepaid Credit Upto
		entity.setMdrUpiPrepaidCreditUptoFixed(dto.getMdrUpiPrepaidCreditUptoFixed());
		entity.setMdrUpiPrepaidCreditUptoPercent(dto.getMdrUpiPrepaidCreditUptoPercent());
		entity.setMdrUpiPrepaidCreditUptoMinimum(dto.getMdrUpiPrepaidCreditUptoMinimum());

		// UPI Prepaid Credit Above
		entity.setMdrUpiPrepaidCreditAboveFixed(dto.getMdrUpiPrepaidCreditAboveFixed());
		entity.setMdrUpiPrepaidCreditAbovePercent(dto.getMdrUpiPrepaidCreditAbovePercent());
		entity.setMdrUpiPrepaidCreditAboveMinimum(dto.getMdrUpiPrepaidCreditAboveMinimum());

		// Additional Fields
		entity.setMdrLeadSource(dto.getMdrLeadSource());
		entity.setMdrDeviceId(dto.getMdrDeviceId());
		entity.setMdrMerCatId(dto.getMdrMerCatId());

		// International AMEX
		entity.setMdrIntlAmexFixed(dto.getMdrIntlAmexFixed());
		entity.setMdrIntlAmexPercent(dto.getMdrIntlAmexPercent());
		entity.setMdrIntlAmexMinimum(dto.getMdrIntlAmexMinimum());

		// Domestic AMEX
		entity.setMdrDomAmexFixed(dto.getMdrDomAmexFixed());
		entity.setMdrDomAmexPercent(dto.getMdrDomAmexPercent());
		entity.setMdrDomAmexMinimum(dto.getMdrDomAmexMinimum());

	    // Continue with your remaining field mappings...
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<Object> getMDRPlanById(Integer id) {

		MDRPlansEntity mdrPlan = mdrPlanRepository.findById(id).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MDR Plan not found with ID: " + id));
		return ResponseEntity.ok(convertToDTO(mdrPlan));
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<Object> getAllMDRPlans() {
		List<MDRPlansEntity> mdrPlans = mdrPlanRepository.findAll();
		List<MDRPlanVO> response = mdrPlans.stream().map(this::convertToDTO).collect(Collectors.toList());
		return ResponseEntity.ok(response);
	}

	@Override
	public String deleteMDRPlan(Integer id) {
		log.info("Deleting MDR Plan ID: {}", id);
		MDRPlansEntity existingPlan = mdrPlanRepository.findByMdrSerNumber(id).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MDR Plan not found with ID: " + id));
		mdrPlanRepository.delete(existingPlan);
		log.info("MDR Plan deleted successfully. ID: {}", id);
		return "MDR Plan deleted successfully. ID: " + id;
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<Object> findAll(int page, int size, String sortBy, String sortDir, String search) {
		Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
		Integer defaultPageNumber = Objects.nonNull(page) ? page - 1 : 0;
		Pageable pageable = PageRequest.of(defaultPageNumber, size, sort);
		Page<MDRPlansEntity> mdrPage;
		if (search != null && !search.trim().isEmpty()) {
			String searchValue = "%" + search.trim().toLowerCase() + "%";
			Specification<MDRPlansEntity> specification = (root, query, cb) -> cb.or(
					cb.like(cb.lower(root.get("mdrPlanDescription")), searchValue),
					cb.like(cb.lower(root.get("mdrType")), searchValue));
			mdrPage = mdrPlanRepository.findAll(specification, pageable);
		} else {
			mdrPage = mdrPlanRepository.findAll(pageable);
		}
		List<MDRPlanVO> voList = mdrPage.getContent().stream().map(this::convertToDTO).toList();
		Map<String, Object> responseVO = new HashMap<>();
		responseVO.put("voList", voList);
		responseVO.put("totalElements", mdrPage.getTotalElements());
		return new ResponseEntity<>(responseVO, HttpStatus.OK);
	}
}
