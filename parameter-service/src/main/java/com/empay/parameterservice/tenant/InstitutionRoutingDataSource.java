package com.empay.parameterservice.tenant;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class InstitutionRoutingDataSource extends AbstractRoutingDataSource {

	@Override
	protected Object determineCurrentLookupKey() {
		return TenantContext.getTenant();
	}
}
