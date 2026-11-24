package com.empay.parameterservice.serviceimpl;

import java.time.LocalDate;
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

import com.empay.parameterservice.entities.AcquirerBinsEntity;
import com.empay.parameterservice.exceptions.ResourceAlreadyExistsException;
import com.empay.parameterservice.exceptions.ResourceNotFoundException;
import com.empay.parameterservice.exceptions.ServiceException;
import com.empay.parameterservice.repository.AcquirerBinsRepository;
import com.empay.parameterservice.service.AcquirerBinsService;
import com.empay.parameterservice.utils.ExceptionUtils;
import com.empay.parameterservice.utils.JwtUtils;
import com.empay.parameterservice.valueobjects.AcquirerBinsVO;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class AcquirerBinsServiceImpl implements AcquirerBinsService {

	private final AcquirerBinsRepository acquirerBinsRepository;
	private final JwtUtils jwtUtils;

	private static final DateTimeFormatter DateFormatTime = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

	private static final String ACQUIRERBINS = "acquirerBins";
	private static final String ACQUIRER_BIN = "Acquirer Bin";
	private static final String ACQUIRER_BIN_WITH_ID = "Acquirer bin with id ";
	private static final String NOT_FOUND = " not found.";

	public ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search) {
		Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
		Integer defaultPageNumber = Objects.nonNull(page) ? page - 1 : 0;
		Pageable pageable = PageRequest.of(defaultPageNumber, size, sort);
		Page<AcquirerBinsEntity> acquirerBInsPage;
		if (search != null && !search.trim().isEmpty()) {
			Specification<AcquirerBinsEntity> specification = (root, query, cb) -> cb.or(
					cb.like(cb.lower(root.get(ACQUIRERBINS)), "%" + search.toLowerCase() + "%"),
					cb.like(cb.lower(root.get("binType")), "%" + search.toLowerCase() + "%"));
			acquirerBInsPage = acquirerBinsRepository.findAll(specification, pageable);
		} else {
			acquirerBInsPage = acquirerBinsRepository.findAll(pageable);
		}
		List<AcquirerBinsVO> voList = acquirerBInsPage.getContent().stream().map(this::mapEntitytoVO).toList();
		Map<String, Object> responseVO = new HashMap<>();
		responseVO.put("voList", voList);
		responseVO.put("totalElements", acquirerBInsPage.getTotalElements());
		return new ResponseEntity<>(responseVO, HttpStatus.OK);
	}

	public AcquirerBinsVO mapEntitytoVO(AcquirerBinsEntity entity) {
		if (entity == null)
			return null;

		AcquirerBinsVO vo = new AcquirerBinsVO();
		vo.setAcquirerBins(entity.getAcquirerBins());
		vo.setLastUpdatedDate(
				entity.getLastUpdatedDate() != null ? entity.getLastUpdatedDate().format(DateFormatTime) : null);
		vo.setUpdatedUser(entity.getUpdatedUser());
		vo.setInstitutionCode(entity.getInstitutionCode());
		vo.setBinType(entity.getBinType());
		vo.setDomIntlFlag(entity.getDomIntlFlag());
		vo.setArnSeqNo(entity.getArnSeqNo());
		vo.setOutfileDate(entity.getOutfileDate() != null ? entity.getOutfileDate().format(DateFormatTime) : null);
		vo.setMcIcaNo(entity.getMcIcaNo());
		vo.setOutBatchNo(entity.getOutBatchNo());
		vo.setOutFileId(entity.getOutFileId());
		vo.setOutFileSeq(entity.getOutFileSeq());
		vo.setParticipantID(entity.getParticipantID());
		vo.setTxnType(entity.getTxnType());

		return vo;
	}

	public ResponseEntity<Object> findById(String id) {
		AcquirerBinsVO acquirerBin = mapEntitytoVO(
				acquirerBinsRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(ACQUIRER_BIN,
						ACQUIRERBINS, ACQUIRER_BIN_WITH_ID + id + NOT_FOUND)));
		return new ResponseEntity<>(acquirerBin, HttpStatus.OK);
	}

	public String saveAcquirerBin(AcquirerBinsVO acquirerBinsVO, HttpServletRequest request) {
		if (acquirerBinsVO.getAcquirerBins() != null
				&& acquirerBinsRepository.existsById(acquirerBinsVO.getAcquirerBins())) {
			throw new ResourceAlreadyExistsException(ACQUIRER_BIN, ACQUIRERBINS,
					ACQUIRER_BIN_WITH_ID + acquirerBinsVO.getAcquirerBins() + " already exists");
		}
		try {
			acquirerBinsVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
			acquirerBinsVO.setInstitutionCode(jwtUtils.getInstitutionCode(request));
			AcquirerBinsEntity entity = mapAcquirerBinsVoToEntity(acquirerBinsVO);
			acquirerBinsRepository.saveAndFlush(entity);
			return "Saved Successfully";
		} catch (DataIntegrityViolationException ex) {
			log.error("Error saveAcquirerBin() ", ex);
			String constraint = ExceptionUtils.extractConstraintName(ex);
			throw mapConstraintToException(constraint);
		} catch (Exception ex) {
			log.error("Error saveAcquirerBin() ", ex);
			throw new ServiceException("Failed to save", ex);
		}
	}

	public AcquirerBinsEntity mapAcquirerBinsVoToEntity(AcquirerBinsVO vo) {
		if (vo == null)
			return null;

		AcquirerBinsEntity entity = new AcquirerBinsEntity();
		entity.setAcquirerBins(vo.getAcquirerBins());
		entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
		entity.setUpdatedUser(vo.getUpdatedUser());
		entity.setInstitutionCode(vo.getInstitutionCode());
		entity.setBinType(vo.getBinType());
		entity.setDomIntlFlag(vo.getDomIntlFlag());
		entity.setArnSeqNo(vo.getArnSeqNo());
		entity.setOutfileDate(
				vo.getOutfileDate() != null ? LocalDate.parse(vo.getOutfileDate(), DateFormatTime).atStartOfDay()
						: null);
		entity.setMcIcaNo(vo.getMcIcaNo());
		entity.setOutBatchNo(vo.getOutBatchNo());
		entity.setOutFileId(vo.getOutFileId());
		entity.setOutFileSeq(vo.getOutFileSeq());
		entity.setParticipantID(vo.getParticipantID());
		entity.setTxnType(vo.getTxnType());

		return entity;
	}

	private void updateAcquirerBinsEntity(AcquirerBinsEntity entity, AcquirerBinsVO vo) {

		entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
		entity.setUpdatedUser(vo.getUpdatedUser());
		entity.setInstitutionCode(vo.getInstitutionCode());
		entity.setBinType(vo.getBinType());
		entity.setDomIntlFlag(vo.getDomIntlFlag());
		entity.setArnSeqNo(vo.getArnSeqNo());

		entity.setOutfileDate(
				vo.getOutfileDate() != null ? LocalDate.parse(vo.getOutfileDate(), DateFormatTime).atStartOfDay()
						: null);

		entity.setMcIcaNo(vo.getMcIcaNo());
		entity.setOutBatchNo(vo.getOutBatchNo());
		entity.setOutFileId(vo.getOutFileId());
		entity.setOutFileSeq(vo.getOutFileSeq());
		entity.setParticipantID(vo.getParticipantID());
		entity.setTxnType(vo.getTxnType());
	}

	public String updateAcquirerBin(AcquirerBinsVO acquirerBinsVO, String id, HttpServletRequest request) {

		try {
			// Validate that body ID matches URL ID
			if (acquirerBinsVO.getAcquirerBins() != null && !id.equals(acquirerBinsVO.getAcquirerBins())) {

				throw new IllegalArgumentException("Invalid request. Please verify the submitted details.");
			}
			// Fetch existing record using URL primary key
			AcquirerBinsEntity existingEntity = acquirerBinsRepository.findById(id)
					.orElseThrow(() -> new ResourceNotFoundException(ACQUIRER_BIN, ACQUIRERBINS,
							ACQUIRER_BIN_WITH_ID + id + NOT_FOUND));

			acquirerBinsVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
			acquirerBinsVO.setInstitutionCode(jwtUtils.getInstitutionCode(request));
			// Update existing entity
			updateAcquirerBinsEntity(existingEntity, acquirerBinsVO);
			acquirerBinsRepository.saveAndFlush(existingEntity);
			return "Updated Successfully";

		} catch (DataIntegrityViolationException ex) {

			log.error("Error updateAcquirerBin() ", ex);
			String constraint = ExceptionUtils.extractConstraintName(ex);
			throw mapConstraintToException(constraint);

		} catch (ResourceNotFoundException | IllegalArgumentException ex) {

			throw ex;

		} catch (Exception ex) {

			log.error("Error updateAcquirerBin() ", ex);
			throw new ServiceException("Failed to update", ex);
		}
	}

	public String deleteAcquirerBin(String id) {
		if (!acquirerBinsRepository.existsById(id)) {
			throw new ResourceNotFoundException(ACQUIRER_BIN, ACQUIRERBINS, ACQUIRER_BIN_WITH_ID + id + NOT_FOUND);
		}
		try {
			acquirerBinsRepository.deleteById(id);
			return "Deleted Successfully";
		} catch (Exception ex) {
			log.error("Error deleteAcquirerBin() ", ex);
			throw new ServiceException("Failed to delete", ex);
		}
	}

	private RuntimeException mapConstraintToException(String constraint) {
		if (constraint == null) {
			return new ResourceAlreadyExistsException(ACQUIRER_BIN, "unknown", "Error");
		}
		return switch (constraint) {
		case "UK_ACQ_BIN" -> new ResourceAlreadyExistsException(ACQUIRER_BIN, "binType",
				"Bin type already exists for this transaction type and institution");
		case "PK_ACQ_BIN" ->
			new ResourceAlreadyExistsException(ACQUIRER_BIN, ACQUIRERBINS, "Acquirer bin already exists");
		default -> new ResourceAlreadyExistsException(ACQUIRER_BIN, "unknown", "Error");
		};
	}
}
