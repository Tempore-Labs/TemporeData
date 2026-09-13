package org.temporedata.modules.sys.agent.entity;

import lombok.Data; import lombok.Builder; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp; import org.hibernate.annotations.GenericGenerator; import org.hibernate.annotations.UpdateTimestamp;
import javax.persistence.*; import java.time.LocalDateTime;

/** Agent entity. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "zy_agent")
public class AgentEntity {

    @Id @GeneratedValue(generator = "uuid2") @GenericGenerator(name = "uuid2", strategy = "uuid2") @Column(length = 36)
    private String id;

    @CreationTimestamp @Column(updatable = false)
    private LocalDateTime createDateTime;

    @Column(length = 32)
    private String createBy;

    @UpdateTimestamp
    private LocalDateTime updateDateTime;

    @Column(length = 32)
    private String updateBy;

    @Column(length = 32)
    private String tenantId;
    @Column
    private String message;
    @Column
    private String context;
    @Column
    private String sessionId;
    @Column
    private String modelName;
    @Column
    private String reply;
    @Column
    private String action;
    @Column
    private Boolean llm;
    @Column
    private String provider;
    @Column
    private String baseUrl;
    @Column
    private String apiKey;
    @Column
    private String model;
    @Column
    private Double temperature;
    @Column
    private Boolean enabled;
    @Column
    private String apiKeyMasked;
    @Column
    private Boolean apiKeySet;
    @Column
    private String title;
    @Column
    private Integer messageCount;
    @Column
    private String lastTime;
    @Column
    private Boolean ok;
}
