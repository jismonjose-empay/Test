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

import com.empay.parameterservice.entities.IFSCodeEntity;
import com.empay.parameterservice.exceptions.ResourceAlreadyExistsException;
import com.empay.parameterservice.exceptions.ResourceNotFoundException;
import com.empay.parameterservice.exceptions.ServiceException;
import com.empay.parameterservice.repository.IFSCodeRepository;
import com.empay.parameterservice.service.IFSCodeService;
import com.empay.parameterservice.utils.ExceptionUtils;
import com.empay.parameterservice.utils.JwtUtils;
import com.empay.parameterservice.valueobjects.IFSCodeVO;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class IFSCodeServiceImpl implements IFSCodeService {

	private final IFSCodeRepository ifsCodeRepository;
	private final JwtUtils jwtUtils;

	private static final String IFSC = "IFSC";
	private static final String OBJ_IFSC = "ifsc";
	private static final String NOT_FOUND = " not found.";

	private static final DateTimeFormatter DateFormatTime = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

	public ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search) {
		Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

		Integer defaultPageNumber = Objects.nonNull(page) ? page - 1 : 0;
		Pageable pageable = PageRequest.of(defaultPageNumber, size, sort);
		Page<IFSCodeEntity> ifscPage;
		if (search != null && !search.trim().isEmpty()) {
			Specification<IFSCodeEntity> specification = (root, query, cb) -> cb.or(
					cb.like(cb.lower(root.get(OBJ_IFSC)), "%" + search.toLowerCase() + "%"),
					cb.like(cb.lower(root.get("micr")), "%" + search.toLowerCase() + "%"),
					cb.like(cb.lower(root.get("bank")), "%" + search.toLowerCase() + "%"),
					cb.like(cb.lower(root.get("branch")), "%" + search.toLowerCase() + "%"),
					cb.like(cb.lower(root.get("city")), "%" + search.toLowerCase() + "%"),
					cb.like(cb.lower(root.get("district")), "%" + search.toLowerCase() + "%"),
					cb.like(cb.lower(root.get("state")), "%" + search.toLowerCase() + "%"));
			ifscPage = ifsCodeRepository.findAll(specification, pageable);
		} else {
			ifscPage = ifsCodeRepository.findAll(pageable);
		}
		List<IFSCodeVO> voList = ifscPage.getContent().stream().map(this::mapIFSCodeEntityToVO).toList();
		Map<String, Object> responseVO = new HashMap<>();
		responseVO.put("voList", voList);
		responseVO.put("totalElements", ifscPage.getTotalElements());
		return new ResponseEntity<>(responseVO, HttpStatus.OK);
	}

	public ResponseEntity<Object> findById(String code) {
		IFSCodeVO ifsc = mapIFSCodeEntityToVO(ifsCodeRepository.findById(code)
				.orElseThrow(() -> new ResourceNotFoundException(IFSC, OBJ_IFSC, IFSC + code + NOT_FOUND)));

		return ResponseEntity.ok(ifsc);
	}

	public IFSCodeVO mapIFSCodeEntityToVO(IFSCodeEntity entity) {
		if (entity == null) {
			return null;
		}
		IFSCodeVO vo = new IFSCodeVO();
		vo.setIfsc(entity.getIfsc());
		vo.setLastUpdatedDate(
				entity.getLastUpdatedDate() != null ? entity.getLastUpdatedDate().format(DateFormatTime) : null);
		vo.setUpdatedUser(entity.getUpdatedUser());
		vo.setMicr(entity.getMicr());
		vo.setBank(entity.getBank());
		vo.setBranch(entity.getBranch());
		vo.setCity(entity.getCity());
		vo.setDistrict(entity.getDistrict());
		vo.setState(entity.getState());
		return vo;
	}

	public IFSCodeEntity mapIFSCodeVOToEntity(IFSCodeVO vo) {
		if (vo == null) {
			return null;
		}
		IFSCodeEntity entity = new IFSCodeEntity();
		entity.setIfsc(vo.getIfsc());
		entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
		entity.setUpdatedUser(vo.getUpdatedUser());
		entity.setMicr(vo.getMicr());
		entity.setBank(vo.getBank());
		entity.setBranch(vo.getBranch());
		entity.setCity(vo.getCity());
		entity.setDistrict(vo.getDistrict());
		entity.setState(vo.getState());
		return entity;
	}

	private void updateIFSCodeEntity(IFSCodeEntity entity, IFSCodeVO vo) {

		entity.setLastUpdatedDate(LocalDateTime.now(ZoneId.systemDefault()));
		entity.setUpdatedUser(vo.getUpdatedUser());

		entity.setMicr(vo.getMicr());
		entity.setBank(vo.getBank());
		entity.setBranch(vo.getBranch());
		entity.setCity(vo.getCity());
		entity.setDistrict(vo.getDistrict());
		entity.setState(vo.getState());
	}

	public String saveIFSC(IFSCodeVO ifsCodeVO, HttpServletRequest request) {
		if (ifsCodeVO.getIfsc() != null && ifsCodeRepository.existsById(ifsCodeVO.getIfsc())) {
			throw new ResourceAlreadyExistsException(IFSC, OBJ_IFSC, IFSC + ifsCodeVO.getIfsc() + " already exists.");
		}
		try {
			ifsCodeVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
			IFSCodeEntity entity = mapIFSCodeVOToEntity(ifsCodeVO);
			ifsCodeRepository.save(entity);
			return "Saved Successfully";
		} catch (DataIntegrityViolationException ex) {
			log.error("Error saveIFSC() ", ex);
			String constraint = ExceptionUtils.extractConstraintName(ex);
			throw mapConstraintToException(constraint);
		} catch (Exception ex) {
			log.error("Error saveIFSC() ", ex);
			throw new ServiceException("Failed to save", ex);
		}
	}

	public String updateIFSC(IFSCodeVO ifsCodeVO, String code, HttpServletRequest request) {
		try {
			if (ifsCodeVO.getIfsc() != null && !code.equalsIgnoreCase(ifsCodeVO.getIfsc())) {
				throw new IllegalArgumentException("Invalid request. Please verify the submitted details.");
			}
			IFSCodeEntity existingEntity = ifsCodeRepository.findById(code).orElseThrow(
					() -> new ResourceNotFoundException(IFSC, "IFSC Code", IFSC + code + NOT_FOUND));
			ifsCodeVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
			updateIFSCodeEntity(existingEntity, ifsCodeVO);
			ifsCodeRepository.save(existingEntity);
			return "Updated Successfully";
		} catch (DataIntegrityViolationException ex) {
			log.error("Error updateIFSC() ", ex);
			String constraint = ExceptionUtils.extractConstraintName(ex);
			throw mapConstraintToException(constraint);
		} catch (ResourceNotFoundException | IllegalArgumentException ex) {
			throw ex;
		} catch (Exception ex) {
			log.error("Error updateIFSC() ", ex);
			throw new ServiceException("Failed to update", ex);
		}
	}

	public String deleteIFSC(String code) {
		if (!ifsCodeRepository.existsById(code)) {
			throw new ResourceNotFoundException(IFSC, OBJ_IFSC, IFSC + code + NOT_FOUND);
		}
		try {
			ifsCodeRepository.deleteById(code);
			return "Deleted Successfully";
		} catch (Exception ex) {
			log.error("Error deleteIFSC() ", ex);
			throw new ServiceException("Failed to delete", ex);
		}
	}

	private RuntimeException mapConstraintToException(String constraint) {
		if (constraint == null) {
			return new ResourceAlreadyExistsException(IFSC, "unknown", "Error");
		}
		return switch (constraint) {
		case "PK_IFS_CODE" -> new ResourceAlreadyExistsException(IFSC, OBJ_IFSC, "IFS code already exists");
		default -> new ResourceAlreadyExistsException(IFSC, "unknown", "Error");
		};
	}
}
