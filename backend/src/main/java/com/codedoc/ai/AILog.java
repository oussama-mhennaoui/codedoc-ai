package com.codedoc.ai;

import com.codedoc.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "ai_logs")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "user")
public class AILog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String endpoint;

    @Column(nullable = false)
    private String status;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer latencyMs;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public AILog(User user, String endpoint, String status, Integer promptTokens, Integer completionTokens, Integer latencyMs) {
        this.user = user;
        this.endpoint = endpoint;
        this.status = status;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.latencyMs = latencyMs;
    }
}
