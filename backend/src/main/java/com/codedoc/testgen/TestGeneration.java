package com.codedoc.testgen;

import com.codedoc.sourcefile.SourceFile;
import com.codedoc.user.User;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "test_generations")
@Getter
@Setter
@NoArgsConstructor
public class TestGeneration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_file_id")
    private SourceFile sourceFile;

    @Column(nullable = false)
    private String language;

    @Column(nullable = false)
    private String testFramework;

    @Column(columnDefinition = "text")
    private String sourceCodeSnippet;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TestGenStatus status = TestGenStatus.PENDING;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode result;

    @Column(columnDefinition = "text")
    private String errorMessage;

    private String modelUsed;
    private String promptVersion;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
