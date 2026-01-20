package com.vule.authen.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vule.authen.configuration.JacksonConfig;
import org.slf4j.Logger;
import org.slf4j.MDC;

public class JsonLogger {

    private static final String LMID_KEY = "lmid";

    private static ApiLog.ApiLogBuilder enrich(ApiLog.ApiLogBuilder builder, String level) {
        String lmid = MDC.get(LMID_KEY);

        return builder
                .level(level)
                .lmid(lmid);
    }

    private static String toJson(Object obj) {
        try {
            return JacksonConfig.MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return String.valueOf(obj);
        }
    }

    public static void info(Logger log, ApiLog.ApiLogBuilder builder) {
        log.info(toJson(enrich(builder, "INFO").build()));
    }

    public static void warn(Logger log, ApiLog.ApiLogBuilder builder) {
        log.warn(toJson(enrich(builder, "WARN").build()));
    }

    public static void error(Logger log, ApiLog.ApiLogBuilder builder, Throwable ex) {
        log.error(toJson(enrich(builder, "ERROR").build()), ex);
    }

    public static void debug(Logger log, ApiLog.ApiLogBuilder builder) {
        log.debug(toJson(enrich(builder, "DEBUG").build()));
    }
}

