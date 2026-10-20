package com.codedoc.quality;

import com.codedoc.sourcefile.SourceFile;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "quality_reports")
@Getter
@Setter
@NoArgsConstructor
public class QualityReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_file_id", nullable = false, unique = true)
    private SourceFile sourceFile;

    @Column(name = "score_global", nullable = false)
    private Integer scoreGlobal;

    @Column(name = "technical_debt_minutes")
    private Integer technicalDebtMinutes = 0;

    @Column(name = "technical_debt_rating", length = 20)
    private String technicalDebtRating = "faible";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_analysis", nullable = false)
    private JsonNode rawAnalysis;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
