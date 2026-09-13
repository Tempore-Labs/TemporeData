package org.temporedata.integration.core.file.entity;

import lombok.Data; import lombok.Builder; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp; import org.hibernate.annotations.GenericGenerator; import org.hibernate.annotations.UpdateTimestamp;
import javax.persistence.*; import java.time.LocalDateTime;

/** File entity. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "zy_file")
public class FileEntity {

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
    private String name;
    @Column
    private String originalName;
    @Column
    private Long fileSize;
    @Column
    private String fileType;
    @Column
    private String createTime;
    @Column(length = 64)
    private String bizType;
    @Column(length = 64)
    private String bizId;
    @Column(length = 32)
    private String ext;
    @Column(length = 64)
    private String md5;
    @Column(length = 255)
    private String storageKey;
    @Column(length = 32)
    private String storageType;
    @Column(length = 16)
    private String status;
    @Column(length = 16)
    private String scanStatus;
    @Column
    private boolean delFlag;
    @Column
    private boolean compressed;
    @Column(length = 16)
    private String compressAlgo;
    @Column
    private Long originalSize;
    @Column
    private Long storedSize;
    @Column
    private boolean imageOptimized;
    @Column
    private boolean packaged;
    @Column
    private Integer memberCount;
    @Column(length = 255)
    private String originalKey;
}
