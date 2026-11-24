package com.empay.parameterservice.serviceimpl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.empay.parameterservice.entities.StatesEntity;
import com.empay.parameterservice.exceptions.ResourceNotFoundException;
import com.empay.parameterservice.repository.StatesRepo;
import com.empay.parameterservice.service.StatesService;
import com.empay.parameterservice.utils.APIConstants;
import com.empay.parameterservice.utils.JwtUtils;
import com.empay.parameterservice.valueobjects.StatesVO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatesServiceImpl implements StatesService {

	private final StatesRepo statesRepo;
	private final JwtUtils jwtUtils;


	private static final DateTimeFormatter DateFormatTime = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

	@Override
	public ResponseEntity<Object> addState(@Valid StatesVO statesVO, HttpServletRequest request) {
		try {
			if (Objects.isNull(statesVO)) {
				return new ResponseEntity<>("Invalid Data", HttpStatus.EXPECTATION_FAILED);
			}
			var entity = statesRepo.findByStateCode(statesVO.getStateCode());
			if (Objects.nonNull(entity)) {
				return new ResponseEntity<>("State code already exists", HttpStatus.CONFLICT);
			}
			statesVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
			statesRepo.saveAndFlush(mapStateVoToEntity(statesVO));
			return new ResponseEntity<>("State Added Successfully", HttpStatus.OK);
		} catch (DataIntegrityViolationException e) {

			log.error("ERROR : addState() :", e);
			String message = e.getRootCause() != null ? e.getRootCause().getMessage() : e.getMessage();
			if (message.contains("UQ_STE_ALPHA_CODE")) {
				return new ResponseEntity<>("Alpha code already exists", HttpStatus.CONFLICT);
			} else if (message.contains("UQ_STE_NAME")) {
				return new ResponseEntity<>("State name already exists", HttpStatus.CONFLICT);
			}
			return new ResponseEntity<>("Duplicate/Invalid data", HttpStatus.CONFLICT);
		} catch (Exception e) {
			log.error("ERROR : addState() :", e);
			return new ResponseEntity<>(APIConstants.COMMON_FAILED_EXCEPTION_RESPONSE,
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private StatesEntity mapStateVoToEntity(@Valid StatesVO vo) {
		StatesEntity entity = new StatesEntity();
		entity.setStateCode(vo.getStateCode());
		entity.setUpdatedUser(vo.getUpdatedUser());
		entity.setLastUpdated(LocalDateTime.now(ZoneId.systemDefault()));
		entity.setStateName(Optional.ofNullable(vo.getStateName()).orElse(""));
		entity.setAlphaCode(Optional.ofNullable(vo.getAlphaCode()).orElse(""));
		return entity;
	}


	private void updateStatesEntity(StatesEntity existingEntity,StatesVO statesVO) {
		existingEntity.setStateName(statesVO.getStateName());
		existingEntity.setAlphaCode(statesVO.getAlphaCode());
		existingEntity.setLastUpdated(LocalDateTime.now(ZoneId.systemDefault()));
		existingEntity.setUpdatedUser(statesVO.getUpdatedUser());
		}

@Override
@Transactional
public ResponseEntity<Object> updateState(@Valid StatesVO statesVO,String id,HttpServletRequest request) {
    try {
        if (statesVO == null || id == null || id.isBlank()) {
            return new ResponseEntity<>("Invalid Data",HttpStatus.EXPECTATION_FAILED);
        }
        if (statesVO.getStateCode() != null && !id.equalsIgnoreCase(statesVO.getStateCode())) {
        		throw new IllegalArgumentException("Invalid request. Please verify the submitted details.");
        }
        StatesEntity existingEntity = statesRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException(
                        "State", "State Code", id));
        statesVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
        updateStatesEntity(existingEntity, statesVO);
        statesRepo.saveAndFlush(existingEntity);
        return new ResponseEntity<>("State Edited Successfully",HttpStatus.OK);
    } catch (DataIntegrityViolationException e) {
        log.error("ERROR : updateState() :", e);
        String message = e.getRootCause() != null? e.getRootCause().getMessage(): e.getMessage();
        if (message != null && message.contains("UQ_STE_ALPHA_CODE")) {
            return new ResponseEntity<>("Alpha code already exists",HttpStatus.CONFLICT);
        } else if (message != null && message.contains("UQ_STE_NAME")) {
            return new ResponseEntity<>("State name already exists",HttpStatus.CONFLICT);
        }
        return new ResponseEntity<>("Duplicate/Invalid data",HttpStatus.CONFLICT);
    } catch (ResourceNotFoundException e) {
        throw e;
    } catch (IllegalArgumentException e) {
        return new ResponseEntity<>(
                "Invalid request. Please verify the submitted details.",HttpStatus.EXPECTATION_FAILED);
    } catch (Exception e) {
        log.error("ERROR : updateState() :", e);
        return new ResponseEntity<>(APIConstants.COMMON_FAILED_EXCEPTION_RESPONSE,
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

	@Override
	public ResponseEntity<Object> getState(String id) {
		var entity = statesRepo.findByStateCode(id);
		if (Objects.nonNull(entity)) {
			
			StatesVO valueObject = mapStatesEntityToVo(entity);
			return new ResponseEntity<>(valueObject, HttpStatus.OK);
		} else {
			return new ResponseEntity<>("Invalid State", HttpStatus.EXPECTATION_FAILED);
		}
	}

	private StatesVO mapStatesEntityToVo(StatesEntity entity) {
		StatesVO vo = new StatesVO();
		vo.setStateCode(entity.getStateCode());
		vo.setUpdatedUser(entity.getUpdatedUser());
		vo.setLastUpdated(Optional.ofNullable(DateFormatTime.format(entity.getLastUpdated())).orElse(""));
		vo.setStateName(Optional.ofNullable(entity.getStateName()).orElse(""));
		vo.setAlphaCode(Optional.ofNullable(entity.getAlphaCode()).orElse(""));
		return vo;
	}

	@Override
	@Transactional
	public ResponseEntity<Object> deleteState(String id, HttpServletRequest request) {
		try {
			var entity = statesRepo.findByStateCode(id);
			if (Objects.nonNull(entity)) {
				statesRepo.delete(entity);
				return new ResponseEntity<>("State Deleted Successfully", HttpStatus.OK);
			} else {
				return new ResponseEntity<>("State not found", HttpStatus.EXPECTATION_FAILED);
			}
		} catch (DataIntegrityViolationException e) {
			log.error("ERROR : deleteState() :", e);
			return new ResponseEntity<>("Cannot delete State. It is referenced in another record.",
					HttpStatus.CONFLICT);

		} catch (Exception e) {
			log.error("ERROR : deleteState() :", e);
			return new ResponseEntity<>(APIConstants.COMMON_FAILED_EXCEPTION_RESPONSE,
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public Object getStates(Integer page, int size, String sortBy, String sortDir, String search) {
		Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

		Integer defaultPageNumber = Objects.nonNull(page) ? page - 1 : 0;
		Pageable pageable = PageRequest.of(defaultPageNumber, size, sort);
		Page<StatesEntity> statesPage;
		if (search != null && !search.trim().isEmpty()) {
			Specification<StatesEntity> specification = (root, query, cb) -> cb.or(
					cb.like(cb.lower(root.get("stateCode")), "%" + search.toLowerCase() + "%"),
					cb.like(cb.lower(root.get("stateName")), "%" + search.toLowerCase() + "%"),
					cb.like(cb.lower(root.get("alphaCode")), "%" + search.toLowerCase() + "%"));
			statesPage = statesRepo.findAll(specification, pageable);
		} else {
			statesPage = statesRepo.findAll(pageable);
		}
		List<StatesVO> voList = statesPage.getContent().stream().map(this::mapStatesEntityToVo).toList();
		Map<String, Object> responseVO = new HashMap<>();
		responseVO.put("voList", voList);
        responseVO.put("totalElements", statesPage.getTotalElements());
		return new ResponseEntity<>(responseVO, HttpStatus.OK);
	}

	@Override
	public Object getAllStates() {
		List<StatesVO> voList = statesRepo.findAll().stream().map(this::mapStatesEntityToVo).toList();
		return new ResponseEntity<>(voList, HttpStatus.OK);
	}

}
