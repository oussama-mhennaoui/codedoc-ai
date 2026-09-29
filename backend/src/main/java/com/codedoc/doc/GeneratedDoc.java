package com.codedoc.doc;

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
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "generated_docs")
@Getter
@Setter
@NoArgsConstructor
public class GeneratedDoc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_file_id")
    private SourceFile sourceFile;

    private String modelUsed;

    private String promptVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode rawResponse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocStatus status;

    @Column(columnDefinition = "text")
    private String errorMessage;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "generatedDoc", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<DocSection> sections = new ArrayList<>();
}
