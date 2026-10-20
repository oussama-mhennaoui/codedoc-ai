package com.codedoc.audit;

import com.codedoc.project.Project;
import com.codedoc.sourcefile.SourceFile;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "security_audits")
@Getter
@Setter
@NoArgsConstructor
public class SecurityAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_file_id")
    private SourceFile sourceFile;

    @Enumerated(EnumType.STRING)
    private AuditStatus status;

    private String securityScore; // A, B, C, D, E, F

    @Column(columnDefinition = "TEXT")
    private String summary;

    @CreationTimestamp
    private Instant createdAt;

    @OneToMany(mappedBy = "audit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Vulnerability> vulnerabilities = new ArrayList<>();

    private String modelUsed;
    
    private String promptVersion;

    @Column(columnDefinition = "TEXT")
    private String rawResponse;
}
