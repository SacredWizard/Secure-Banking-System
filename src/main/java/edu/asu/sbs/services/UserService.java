package edu.asu.sbs.services;

import edu.asu.sbs.config.UserType;
import edu.asu.sbs.errors.EmailAlreadyUsedException;
import edu.asu.sbs.errors.PhoneNumberAlreadyUsedException;
import edu.asu.sbs.errors.SsnAlreadyUsedException;
import edu.asu.sbs.errors.UsernameAlreadyUsedException;
import edu.asu.sbs.models.Account;
import edu.asu.sbs.models.Transaction;
import edu.asu.sbs.models.User;
import edu.asu.sbs.repositories.AccountRepository;
import edu.asu.sbs.repositories.TransactionRepository;
import edu.asu.sbs.repositories.UserRepository;
import edu.asu.sbs.security.jwt.JWTFilter;
import edu.asu.sbs.security.jwt.TokenProvider;
import edu.asu.sbs.services.dto.UserDTO;
import edu.asu.sbs.util.RandomUtil;
import edu.asu.sbs.vm.LoginVM;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Calendar;
import java.util.Optional;

@Slf4j
@Service
public class UserService {

    final UserRepository userRepository;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final TokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    final AccountRepository accountRepository;
    final TransactionRepository transactionRepository;


    public UserService(UserRepository userRepository,AccountRepository accountRepository, TransactionRepository transactionRepository, AuthenticationManagerBuilder authenticationManagerBuilder, TokenProvider tokenProvider, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public void createUpdateUser(User user) {

        User u = new User();
        u.setUserName("admin");
        u.setFirstName("R");
        u.setLastName("R");
        u.setPhoneNumber("9994621912");
        u.setSsn("123-45-6789");
        u.setDateOfBirth(new Date(Calendar.getInstance().getTime().getTime()));
        u.setEmail("93@asu,edu");
        u.setPasswordHash(passwordEncoder.encode("admin"));
        u.setUserType("t2");
        userRepository.save(u);

        u = new User();
        u.setUserName("user");
        u.setFirstName("K");
        u.setLastName("K");
        u.setPhoneNumber("7708316840");
        u.setSsn("123-45-5675");
        u.setDateOfBirth(new Date(Calendar.getInstance().getTime().getTime()));
        u.setEmail("7676@asu.edu");
        u.setPasswordHash(passwordEncoder.encode("user"));
        u.setUserType("t2");
        userRepository.save(u);

        Account a= new Account();
        a.setAccountBalance(1000.00);
        a.setAccountNumber("12345");
        a.setAccountType("savings");
        a.setActive(true);
        a.setUser(userRepository.findOneWithUserTypeByUserName("admin").orElse(null));
        accountRepository.save(a);

        a= new Account();
        a.setAccountBalance(1000.00);
        a.setAccountNumber("12347");
        a.setAccountType("checking");
        a.setActive(true);
        a.setUser(userRepository.findOneWithUserTypeByUserName("admin").orElse(null));
        accountRepository.save(a);

        a= new Account();
        a.setAccountBalance(1000.00);
        a.setAccountNumber("12346");
        a.setAccountType("checking");
        a.setActive(true);
        a.setUser(userRepository.findOneWithUserTypeByUserName("user").orElse(null));
        accountRepository.save(a);

        Transaction t = new Transaction();
        t.setCreatedTime(new Timestamp(System.currentTimeMillis()));
        t.setDescription("Dummy transfer");
        t.setStatus("SUCCESS");
        t.setTransactionAmount(100.00);
        t.setUpdatedTime(new Timestamp(System.currentTimeMillis()));
        t.setTransactionType("Internal");
        t.setFromAccount(accountRepository.findOneByAccountNumberEquals("12346").orElse(null));
        t.setToAccount(accountRepository.findOneByAccountNumberEquals("12347").orElse(null));
        transactionRepository.save(t);

    }


    public ResponseEntity<JWTToken> authenticate(LoginVM loginVM) {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(loginVM.getUserName(), loginVM.getPassword());
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(usernamePasswordAuthenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        boolean rememberMe = loginVM.getRememberMe() != null;
        String jwt = tokenProvider.createToken(authentication, rememberMe);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + jwt);
        return new ResponseEntity<>(new JWTToken(jwt), httpHeaders, HttpStatus.OK);
    }

    @Transactional
    public void registerUser(UserDTO userDTO, String password) {
        validateUserDTO(userDTO);
        User user = new User();
        user.setUserName(userDTO.getUserName().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setEmail(userDTO.getEmail().toLowerCase());
        user.setActive(false);
        user.setActivationKey(RandomUtil.generateActivationKey());
        user.setDateOfBirth(userDTO.getDateOfBirth());
        user.setSsn(userDTO.getSsn());
        user.setUserType(UserType.USER_ROLE);
        user.setPhoneNumber(userDTO.getPhoneNumber());
        userRepository.save(user);
    }

    private void validateUserDTO(UserDTO userDTO) {
        userRepository.findOneByUserNameOrEmailIgnoreCaseOrSsnOrPhoneNumber(userDTO.getUserName().toLowerCase(), userDTO.getEmail(), userDTO.getSsn(), userDTO.getPhoneNumber()).
                ifPresent(existingUser -> {
                    if (!removeNonActivatedUser(existingUser)) {
                        if (existingUser.getEmail().equalsIgnoreCase(userDTO.getEmail().toLowerCase())) {
                            throw new EmailAlreadyUsedException();
                        } else if (existingUser.getPhoneNumber().equals(userDTO.getPhoneNumber())) {
                            throw new PhoneNumberAlreadyUsedException();
                        } else if (existingUser.getSsn().equals(userDTO.getSsn())) {
                            throw new SsnAlreadyUsedException();
                        } else if (existingUser.getUserName().equalsIgnoreCase(userDTO.getUserName().toLowerCase())) {
                            throw new UsernameAlreadyUsedException();
                        }
                    }
                });
    }

    private boolean removeNonActivatedUser(User existingUser) {
        if (existingUser.isActive()) {
            return false;
        }
        userRepository.delete(existingUser);
        userRepository.flush();
        return true;
    }

    @Transactional
    public Optional<User> activateRegistration(String key) {
        log.debug("Activating user for activation key {}", key);
        return userRepository.findOneByActivationKey(key)
                .map(user -> {
                    user.setActive(true);
                    user.setActivationKey(null);
                    userRepository.save(user);
                    log.debug("Activated user: {}", user);
                    return user;
                });
    }

    @Transactional
    public Optional<User> requestPasswordReset(String email) {
        return userRepository.findOneByEmailIgnoreCase(email)
                .filter(User::isActive)
                .map(user -> {
                    user.setResetKey(RandomUtil.generateResetKey());
                    user.setResetDate(Instant.now());
                    userRepository.save(user);
                    return user;
                });
    }

    @Transactional
    public Optional<User> completePasswordReset(String newPassword, String key) {
        log.debug("Reset user password for reset key {}", key);
        return userRepository.findOneByResetKey(key)
                .filter(user -> user.getResetDate().isAfter(Instant.now().minusSeconds(86400)))
                .map(user -> {
                    user.setPasswordHash(passwordEncoder.encode(newPassword));
                    user.setResetKey(null);
                    user.setResetDate(null);
                    userRepository.save(user);
                    return user;
                });
    }

    @Getter
    @Setter
    public static class JWTToken {
        private String token;

        JWTToken(String token) {
            this.token = token;
        }
    }
}
