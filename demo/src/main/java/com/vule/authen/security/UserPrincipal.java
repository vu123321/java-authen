//package com.vule.authen.security;
//
//import com.vule.authen.entity.User;
//import lombok.Getter;
//import org.jspecify.annotations.Nullable;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.userdetails.UserDetails;
//
//import java.util.Collection;
//import java.util.List;
//
//public class UserPrincipal implements UserDetails {
//    @Getter
//    private Long id;
//    private final String username;
//    @Getter
//    private String email;
//    @Getter
//    private String phoneNumber;
//    private final String password;
//
//    private Collection<? extends GrantedAuthority> authorities;
//
//    public UserPrincipal(Long id, String username, String email, String phoneNumber, String password,
//                         Collection<? extends GrantedAuthority> authorities) {
//        this.id = id;
//        this.username = username;
//        this.email = email;
//        this.phoneNumber = phoneNumber;
//        this.password = password;
//        this.authorities = authorities;
//    }
//
//    public static UserPrincipal create(User user) {
//        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
//
//        return new UserPrincipal(
//                user.getId(),
//                user.getUsername(),
//                user.getLastName(),
//                user.getFirstName(),
//                user.getPassword(),
//                authorities);
//    }
//
//    @Override
//    public Collection<? extends GrantedAuthority> getAuthorities() {
//        return authorities;
//    }
//
//    @Override
//    public @Nullable String getPassword() {
//        return password;
//    }
//
//    @Override
//    public String getUsername() {
//        return username;
//    }
//
//    @Override
//    public boolean isAccountNonExpired() {
//        return true;
//    }
//
//    @Override
//    public boolean isAccountNonLocked() {
//        return true;
//    }
//
//    @Override
//    public boolean isCredentialsNonExpired() {
//        return true;
//    }
//
//    @Override
//    public boolean isEnabled() {
//        return true;
//    }
//}
