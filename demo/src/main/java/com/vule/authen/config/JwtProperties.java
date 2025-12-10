package com.vule.authen.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String issuer = "http://localhost:8080";
    private int accessTokenExpirationMinutes = 30;
    private int refreshTokenExpirationDay = 1;
}
