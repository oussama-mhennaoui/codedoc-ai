package com.codedoc.sourcefile;

import com.codedoc.project.Project;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "source_files", uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "filename"}))
@Getter
@Setter
@NoArgsConstructor
public class SourceFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false)
    private String filename;

    @Column(columnDefinition = "text", nullable = false)
    private String content;

    private String language;

    private Long sizeBytes;

    private String checksum;

    @CreationTimestamp
    private Instant uploadedAt;
}
