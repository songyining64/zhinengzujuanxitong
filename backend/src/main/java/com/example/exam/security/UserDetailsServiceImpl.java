package com.example.exam.security;

import com.example.exam.module.system.entity.User;
import com.example.exam.module.system.service.SysMenuService;
import com.example.exam.module.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserService userService;
    private final SysMenuService sysMenuService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
        for (String p : sysMenuService.listGrantedPermsForRole(user.getRole())) {
            if (StringUtils.hasText(p)) {
                authorities.add(new SimpleGrantedAuthority(p));
            }
        }
        Collection<? extends GrantedAuthority> auths = authorities;
        boolean enabled = user.getStatus() != null && user.getStatus() == 1;
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                enabled,
                true,
                true,
                true,
                auths
        );
    }
}

