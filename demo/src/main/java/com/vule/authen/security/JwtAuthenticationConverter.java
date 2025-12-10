//package com.vule.authen.security;
//
//import org.jspecify.annotations.Nullable;
//import org.springframework.core.convert.converter.Converter;
//import org.springframework.security.authentication.AbstractAuthenticationToken;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.oauth2.jwt.Jwt;
//import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
//import org.springframework.stereotype.Component;
//
//import java.util.Collection;
//import java.util.Collections;
//
//@Component
//public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
//
//    @Override
//    public AbstractAuthenticationToken convert(Jwt jwt) {
//        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
//        return new JwtAuthenticationToken(jwt, authorities);
//    }
//
//    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
//        // For our simple case, all users get ROLE_USER
//        // You can customize this to extract roles from JWT claims if needed
//        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
//    }
//}
