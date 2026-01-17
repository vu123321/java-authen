package com.vule.authen.utils;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.UUID;

public class JsonLogger {

    private static final Gson gson = new Gson();
    private static final String LMID_KEY = "lmid";

    private static ApiLog.ApiLogBuilder enrich(ApiLog.ApiLogBuilder builder, String level) {
        String lmid = MDC.get(LMID_KEY);

//        if (lmid == null) {
//            lmid = UUID.randomUUID().toString();
//            MDC.put(LMID_KEY, lmid);
//        }

        return builder
                .level(level)
                .lmid(lmid);
    }

    public static void info(Logger log, ApiLog.ApiLogBuilder builder) {
        log.info(gson.toJson(enrich(builder, "INFO").build()));
    }

    public static void warn(Logger log, ApiLog.ApiLogBuilder builder) {
        log.warn(gson.toJson(enrich(builder, "WARN").build()));
    }

    public static void error(Logger log, ApiLog.ApiLogBuilder builder, Throwable ex) {
        log.error(gson.toJson(enrich(builder, "ERROR").build()), ex);
    }

    public static void debug(Logger log, ApiLog.ApiLogBuilder builder) {
        log.debug(gson.toJson(enrich(builder, "DEBUG").build()));
    }
}

