package com.empay.mars_gateway.valueobjects;

import lombok.Data;

@Data
public class UserActivityTrackingVO {

    private Integer serialNumber;

    private String eventType;

    private String actorUserId;

    private String resourceType;

    private String resourceId;

    private String correlationId;

    private String dateTime;

    private String detail;

    private String sourceIp;

    private String sessionId;

    private String rowHash;

    private String prevHash;

    private String userName;

    private String eventTypeDesc;

    private String tampered;

    private String response;

    private String responseDetail;

    private Integer responseStatus;
}