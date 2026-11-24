package com.empay.parameterservice.entities;

import java.io.Serializable;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class UserRoleIdV2 implements Serializable {
	private Integer serialNumber;
	private String roleCode;
}
