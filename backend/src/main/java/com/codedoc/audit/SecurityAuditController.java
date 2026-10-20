package com.codedoc.audit;

import com.codedoc.audit.dto.SecurityAuditDto;
import com.codedoc.audit.dto.SecurityAuditRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class SecurityAuditController {

    private final SecurityAuditService auditService;
    private final SecurityAuditExportService exportService;

    @PostMapping
    public ResponseEntity<SecurityAuditDto> performAudit(@RequestBody SecurityAuditRequest request) {
        return ResponseEntity.ok(auditService.performAudit(request));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<SecurityAuditDto>> getProjectAudits(@PathVariable Long projectId) {
        return ResponseEntity.ok(auditService.getAuditsByProject(projectId));
    }

    @GetMapping("/file/{fileId}")
    public ResponseEntity<List<SecurityAuditDto>> getFileAudits(@PathVariable Long fileId) {
        return ResponseEntity.ok(auditService.getAuditsByFile(fileId));
    }

    @GetMapping("/{auditId}")
    public ResponseEntity<SecurityAuditDto> getAudit(@PathVariable Long auditId) {
        return ResponseEntity.ok(auditService.getAudit(auditId));
    }

    @GetMapping("/{auditId}/export/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long auditId) {
        try {
            SecurityAuditDto audit = auditService.getAudit(auditId);
            byte[] pdf = exportService.toPdf(audit);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=security-audit-" + auditId + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Error exporting PDF for audit {}: ", auditId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
