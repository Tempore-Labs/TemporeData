package org.temporedata.modules.sys.message.entity;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;
import javax.persistence.*;
import java.time.LocalDateTime;

/** Message entity. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "zy_message")
public class MessageEntity {
    @Id @GeneratedValue(generator="uuid2") @GenericGenerator(name="uuid2", strategy="uuid2") @Column(length=32)
    private String id;
    @CreationTimestamp @Column(updatable=false)
    private LocalDateTime createDateTime;
    @Column(length=32)
    private String createBy;
    @UpdateTimestamp
    private LocalDateTime updateDateTime;
    @Column(length=32)
    private String updateBy;
    @Column(length=32)
    private String tenantId;
    @Column
    private String name;
    @Column
    private String status;
    @Column(columnDefinition="TEXT")
    private String description;

    /** Frontend-friendly title maps to name. */
    public String getTitle() {
        return name;
    }

    /** Frontend-friendly type maps to description when present, else a generic tag. */
    public String getType() {
        return description == null || description.isBlank() ? "通知" : description;
    }

    /** Frontend-friendly read flag (1 = read, 0 = unread). */
    public int getIsRead() {
        return "READ".equals(status) ? 1 : 0;
    }

    /** Frontend-friendly time string. */
    public String getCreateTime() {
        return createDateTime == null ? "" : createDateTime.toString().replace("T", " ");
    }
}
