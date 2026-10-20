package com.codedoc.audit.dto;

import lombok.Data;

@Data
public class SecurityAuditRequest {
    private Long projectId;
    private Long fileId;
}
