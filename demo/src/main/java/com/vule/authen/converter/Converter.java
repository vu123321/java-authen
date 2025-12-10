//package com.vule.authen.converter;
//
//
//import org.modelmapper.ModelMapper;
//import org.modelmapper.convention.NameTokenizers;
//import org.springframework.context.annotation.Bean;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
//import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
//import org.springframework.stereotype.Component;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Component
//public class Converter {
//
//    public static <T> T toModel(Object obj,Class<T> zClass) {
//        ModelMapper modelMapper = new ModelMapper();
//        modelMapper.getConfiguration().setSourceNameTokenizer(NameTokenizers.UNDERSCORE);
//        return (T) modelMapper.map(obj,zClass);
//    }
//
//    public static <T,Y> List<T> toList(List<Y> list, Class<T> zClass) {
//        return list.stream().map(e-> toModel(e,zClass)).collect(Collectors.toList());
//    }
//
//}
