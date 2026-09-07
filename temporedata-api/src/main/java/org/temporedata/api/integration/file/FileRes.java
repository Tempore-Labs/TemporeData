package org.temporedata.api.integration.file;

import lombok.Data;

@Data
public class FileRes {

    private String id;

    private String name;

    private String originalName;

    private Long fileSize;

    private String fileType;

    private String ext;

    private String md5;

    private String bizType;

    private String bizId;

    private String createBy;

    private String createTime;

    private boolean imageOptimized;

    private boolean packaged;

    private Integer memberCount;
}