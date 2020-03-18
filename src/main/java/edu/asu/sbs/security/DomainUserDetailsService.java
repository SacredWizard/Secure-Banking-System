package edu.asu.sbs.security;

import com.google.common.collect.Lists;
import edu.asu.sbs.repositories.UserRepository;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.internal.constraintvalidators.bv.EmailValidator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Slf4j
@Component("userDetailsService")
public class DomainUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DomainUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) {
        log.debug("Authenticating {}", username);

        if (new EmailValidator().isValid(username, null)) {
            return userRepository.findOneWithAuthoritiesByEmailIgnoreCase(username)
                    .map(user -> createSpringSecurityUser(username, user))
                    .orElseThrow(() -> new UsernameNotFoundException("User with email "+ username+" does not exist"));
        }

        String lowercaselogin = username.toLowerCase(Locale.ENGLISH);
        return userRepository.findOneWithUserTypeByUserName(lowercaselogin)
                .map(user -> createSpringSecurityUser(lowercaselogin, user))
                .orElseThrow(() -> new UsernameNotFoundException("User "+lowercaselogin+" does not exist"));

    }

    @SneakyThrows
    private User createSpringSecurityUser(String lowercaseLogin, edu.asu.sbs.models.User user) {
        if (!user.isActive()) {
            throw new UserNotActivatedException("User "+lowercaseLogin+" was not activated");
        }

        List<GrantedAuthority> grantedAuthorities = Lists.newArrayList();
        grantedAuthorities.add(new SimpleGrantedAuthority(user.getUserType()));

        return new User(user.getUserName(), user.getPasswordHash(), grantedAuthorities);
    }
}
