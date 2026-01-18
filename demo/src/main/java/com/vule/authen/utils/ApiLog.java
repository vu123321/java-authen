package com.vule.authen.utils;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiLog {

    // tracking
    private String lmid;
    private String level;
    private String type;

    // main message
    private String message;

    // HTTP info
    private String method;
    private String url;
    private Integer status;

    // body
    private Object requestBody;
    private Object responseBody;

    // business context
    private String username;
    private Object context;

    // performance
    private Long duration;
    private String lineCode;
}



