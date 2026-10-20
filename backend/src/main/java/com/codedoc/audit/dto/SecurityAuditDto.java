package com.codedoc.audit.dto;

import com.codedoc.audit.AuditStatus;
import com.codedoc.audit.Severity;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class SecurityAuditDto {
    private Long id;
    private Long projectId;
    private Long sourceFileId;
    private String fileName;
    private AuditStatus status;
    private String securityScore;
    private String summary;
    private Instant createdAt;
    private List<VulnerabilityDto> vulnerabilities;
}
