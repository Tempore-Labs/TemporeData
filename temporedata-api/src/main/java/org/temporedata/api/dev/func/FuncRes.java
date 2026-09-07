package org.temporedata.api.dev.func;

import lombok.Data;

@Data
public class FuncRes {

    private String id;

    private String name;

    private String type;

    private String language;

    private String description;

    private String status;

    private String createTime;
}