package com.vule.authen.lifecycle;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

@Component
public class ShutdownHook {

    @PreDestroy
    public void onShutdown() {
        System.out.println(" --------------------------------Graceful shutdown started !!!!!!!!--------------------------------");
    }
}
