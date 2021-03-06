package edu.asu.sbs.services;

import com.google.common.collect.Lists;
import edu.asu.sbs.config.Constants;
import edu.asu.sbs.config.TransactionStatus;
import edu.asu.sbs.config.TransactionType;
import edu.asu.sbs.config.UserType;
import edu.asu.sbs.errors.*;
import edu.asu.sbs.globals.AccountType;
import edu.asu.sbs.models.*;
import edu.asu.sbs.repositories.*;
import edu.asu.sbs.security.jwt.JWTFilter;
import edu.asu.sbs.security.jwt.TokenProvider;
import edu.asu.sbs.services.dto.NewAccountRequestDTO;
import edu.asu.sbs.services.dto.TransactionDTO;
import edu.asu.sbs.services.dto.TransferOrRequestDTO;
import edu.asu.sbs.services.dto.UserDTO;
import edu.asu.sbs.util.RandomUtil;
import edu.asu.sbs.vm.LoginVM;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class UserService {

    final UserRepository userRepository;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final TokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionAccountLogRepository transactionAccountLogRepository;
    private final OTPService otpService;
    private final AccountService accountService;
    private final SessionRepository sessionRepository;


    public UserService(UserRepository userRepository, AccountRepository accountRepository, TransactionRepository transactionRepository, AuthenticationManagerBuilder authenticationManagerBuilder, TokenProvider tokenProvider, PasswordEncoder passwordEncoder, TransactionAccountLogRepository transactionAccountLogRepository, OTPService otpService, AccountService accountService, SessionRepository sessionRepository) {
        this.userRepository = userRepository;
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transactionAccountLogRepository = transactionAccountLogRepository;
        this.otpService = otpService;
        this.accountService = accountService;
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public void createUpdateUser() {

        User u = new User();
        u.setUserName("tier2");
        u.setFirstName("R");
        u.setLastName("R");
        u.setPhoneNumber("9994621912");
        u.setSsn("123-45-6789");
        u.setDateOfBirth(new Date(Calendar.getInstance().getTime().getTime()));
        u.setEmail("93@asu.edu");
        u.setPasswordHash(passwordEncoder.encode("tier2"));
        u.setUserType(UserType.EMPLOYEE_ROLE2);
        u.setActive(true);
        userRepository.save(u);

        u = new User();
        u.setUserName("admin");
        u.setActive(true);
        u.setFirstName("K");
        u.setLastName("K");
        u.setPhoneNumber("7708316841");
        u.setSsn("123-45-5674");
        u.setDateOfBirth(new Date(Calendar.getInstance().getTime().getTime()));
        u.setEmail("76761@asu.edu");
        u.setPasswordHash(passwordEncoder.encode("admin"));
        u.setUserType(UserType.ADMIN_ROLE);
        userRepository.save(u);

        u = new User();
        u.setUserName("tier1");
        u.setActive(true);
        u.setFirstName("K");
        u.setLastName("K");
        u.setPhoneNumber("7708316840");
        u.setSsn("123-45-5675");
        u.setDateOfBirth(new Date(Calendar.getInstance().getTime().getTime()));
        u.setEmail("7676@asu.edu");
        u.setPasswordHash(passwordEncoder.encode("tier1"));
        u.setUserType(UserType.EMPLOYEE_ROLE1);
        userRepository.save(u);

        u = new User();
        u.setUserName("user1");
        u.setActive(true);
        u.setFirstName("K");
        u.setLastName("K");
        u.setPhoneNumber("6708316840");
        u.setSsn("123-45-5775");
        u.setDateOfBirth(new Date(Calendar.getInstance().getTime().getTime()));
        u.setEmail("776@asu.edu");
        u.setPasswordHash(passwordEncoder.encode("user1"));
        u.setUserType(UserType.USER_ROLE);
        userRepository.save(u);

        u = new User();
        u.setUserName("user2");
        u.setActive(true);
        u.setFirstName("K");
        u.setLastName("K");
        u.setPhoneNumber("6508316840");
        u.setSsn("123-45-5785");
        u.setDateOfBirth(new Date(Calendar.getInstance().getTime().getTime()));
        u.setEmail("7746@asu.edu");
        u.setPasswordHash(passwordEncoder.encode("user2"));
        u.setUserType(UserType.USER_ROLE);
        userRepository.save(u);

        Account a = new Account();
        a.setAccountBalance(1000.00);
        a.setAccountType(AccountType.SAVINGS);
        a.setActive(true);
        a.setUser(userRepository.findOneWithUserTypeByUserName("user2").orElse(null));
        accountRepository.save(a);

        a = new Account();
        a.setAccountBalance(1000.00);
        a.setAccountType(AccountType.CHECKING);
        a.setActive(true);
        a.setUser(userRepository.findOneWithUserTypeByUserName("user2").orElse(null));
        accountRepository.save(a);

        Account b = new Account();
        b.setAccountBalance(1000.00);
        b.setAccountType(AccountType.CHECKING);
        b.setActive(true);
        b.setUser(userRepository.findOneWithUserTypeByUserName("user1").orElse(null));
        accountRepository.save(b);

        Transaction t = new Transaction();
        t.setCreatedTime(Instant.now());
        t.setDescription("Dummy transfer");
        t.setStatus(TransactionStatus.APPROVED);
        t.setTransactionAmount(100.0);
        t.setModifiedTime(Instant.now());
        t.setTransactionType(TransactionType.DEBIT);
        t.setFromAccount(accountRepository.findById(a.getId()).orElse(null));
        t.setToAccount(accountRepository.findById(b.getId()).orElse(null));
        TransactionAccountLog transactionAccountLog = new TransactionAccountLog();
        transactionAccountLog.setLogDescription(t.getDescription());
        TransactionAccountLog tlog = transactionAccountLogRepository.save(transactionAccountLog);
        t.setLog(tlog);
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
        this.startSession();
        return new ResponseEntity<>(new JWTToken(jwt), httpHeaders, HttpStatus.OK);
    }

    @Transactional
    public User registerUser(UserDTO userDTO, String password) {
        return registerUser(userDTO, password, UserType.USER_ROLE);
    }

    @Transactional
    public User registerUser(UserDTO userDTO, String password, String userType) {
        validateUserDTO(userDTO);
        validateUserType(userType);
        User user = new User();
        user.setUserName(userDTO.getUserName().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setEmail(userDTO.getEmail().toLowerCase());
        user.setActive(false);
        user.setUserType(userType);
        user.setActivationKey(RandomUtil.generateActivationKey());
        user.setDateOfBirth(userDTO.getDateOfBirth());
        user.setSsn(userDTO.getSsn());
        user.setPhoneNumber(userDTO.getPhoneNumber());
        log.info(Instant.now() + ": Created User: " + user.toString());
        userRepository.save(user);
        return user;
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

    private void validateUserType(String userType) {
        if (!(userType.equals(UserType.ADMIN_ROLE) || userType.equals(UserType.EMPLOYEE_ROLE1) || userType.equals(UserType.EMPLOYEE_ROLE2) || userType.equals(UserType.USER_ROLE) || userType.equals(UserType.MERCHANT_ROLE))) {
            throw new UserTypeException();
        }
    }

    private boolean removeNonActivatedUser(User existingUser) {
        if (existingUser.isActive()) {
            return false;
        }
        userRepository.delete(existingUser);
        userRepository.flush();
        log.info(Instant.now() + ": Flushed Inactive Users");
        return true;
    }

    @Transactional
    public Optional<User> activateRegistration(String key) {
        Optional<User> optionalUser = userRepository.findOneByActivationKey(key);
        log.debug("Activating user for activation key {}", key);
        return optionalUser
                .map(user -> {
                    user.setActive(true);
                    user.setActivationKey(null);
                    user = userRepository.save(user);
                    log.debug(Instant.now() + ": Activated user: {}", user);
                    if (user.getUserType().equals(UserType.MERCHANT_ROLE)) {
                        NewAccountRequestDTO newAccountRequestDTO = new NewAccountRequestDTO();
                        newAccountRequestDTO.setAccountType(AccountType.CURRENT);
                        newAccountRequestDTO.setInitialDeposit(Constants.INITIAL_DEPOSIT_AMOUNT);
                        newAccountRequestDTO = accountService.createAccount(user, newAccountRequestDTO);
                        Optional<Account> optionalAccount = accountRepository.findById(Long.valueOf(newAccountRequestDTO.getAccountNumber()));
                        optionalAccount.ifPresent(account -> {
                            account.setDefaultAccount(true);
                            accountRepository.save(account);
                        });
                    }
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
                    log.info(Instant.now() + ": Password reset for user. user: " + user.toString());
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
                    log.info(Instant.now() + ": Completed Password reset for user. userID: " + user.getId());
                    userRepository.save(user);
                    return user;
                });
    }

    public void updateUserType(User requestBy, String userType) {

        requestBy.setUserType(userType);
        log.info(Instant.now() + ": Updated user. userID: " + requestBy.getId());
        userRepository.save(requestBy);
    }

    public Object getAllUsers() {
        return userRepository.findByUserTypeInAndIsActive(Lists.newArrayList(UserType.USER_ROLE), true);
    }

    public void startSession() {
        Instant sessionStart = Instant.now();
        Session session = new Session();
        session.setLinkedUser(getCurrentUser());
        session.setSessionStart(Instant.now());
        session.setSessionTimeout(Constants.EXPIRE_MINS);
        session.setSessionEnd(sessionStart.plus(Constants.EXPIRE_MINS, ChronoUnit.MINUTES));
        //session.setIp();
        sessionRepository.save(session);
    }

    public void endSession() {
        Session session = sessionRepository.findByLinkedUser(getCurrentUser());
        session.setSessionEnd(Instant.now());
        sessionRepository.save(session);
    }
    @Getter
    @Setter
    public static class JWTToken {
        private String token;

        JWTToken(String token) {
            this.token = token;
        }
    }

    public User getCurrentUser() {
        log.info("Getting current logged in user");
        String currentUserName = "";
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            currentUserName = authentication.getName();
        }
        log.debug(Instant.now() + ": Logged in User: '{}'", currentUserName);
        return userRepository.findOneByUserName(currentUserName).orElse(null);
    }

    @Transactional
    public void editUser(UserDTO userDTO) {
        Optional<User> current = userRepository.findById(userDTO.getId());
        current.ifPresent(user -> {
            if (!userDTO.getPhoneNumber().isEmpty()) {
                System.out.println("Changing phone num");
                user.setPhoneNumber(userDTO.getPhoneNumber());
            }
            if (!userDTO.getUserName().isEmpty()) {
                System.out.println("Changing username");
                user.setUserName(userDTO.getUserName());
            }
            if (!userDTO.getFirstName().isEmpty()) {
                System.out.println("Changing firstname");
                user.setFirstName(userDTO.getFirstName());
            }
            if (!userDTO.getLastName().isEmpty()) {
                System.out.println("Changing lastname");
                user.setLastName(userDTO.getLastName());
            }
            if (!userDTO.getEmail().isEmpty()) {
                System.out.println("Changing email");
                user.setEmail(userDTO.getEmail());
            }
            user.setActive(true);
            userRepository.save(user);
            log.info(Instant.now() + ": Updated user from userDTO. userID: " + user.getId());
        });
    }


    public List<User> getAllEmployees() {
        return userRepository.findByUserTypeInAndIsActive(Lists.newArrayList(UserType.EMPLOYEE_ROLE1, UserType.EMPLOYEE_ROLE2), true);
    }

    public User getUserByIdAndActive(Long id) {
        log.info("Getting user by id and isActive=true");
        Optional<User> optionalUser = userRepository.findByIdAndIsActive(id, true);
        return optionalUser.orElse(null);
    }

    @Transactional
    public void deleteUser(Long id) {
        Optional<User> current = userRepository.findById(id);
        current.ifPresent(user -> {
            user.setActive(false);
            user.setExpireOn(Instant.now());
            userRepository.save(user);
            log.info(Instant.now() + ": Deactivated user. userID: " + user.getId());
        });
    }


    public String logout(HttpServletRequest request, HttpServletResponse response) {
        endSession();
        User user = getCurrentUser();
        otpService.clearOTP(user.getEmail());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            otpService.clearOTP(user.getEmail());
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        return "redirect:/login?logout";
    }

    public User getUserByUserName(String userName){
        return userRepository.findOneByUserName(userName).get();
    }

    public User getUserByEmail(String email){
        return userRepository.findOneByEmailAndIsActive(email,true).get();
    }

    public User getUserByPhoneNumber(String phoneNumber){
        return userRepository.findOneByPhoneNumberAndIsActive(phoneNumber,true).get();
    }

    public TransactionDTO transferByEmailOrPhone(TransferOrRequestDTO transferOrRequestDTO){
        User user = this.getCurrentUser();
        TransactionDTO transactionDTO = new TransactionDTO();
        transactionDTO.setFromAccount(transferOrRequestDTO.getFromAccount());
        transactionDTO.setDescription(transferOrRequestDTO.getDescription());
        transactionDTO.setTransactionAmount(transferOrRequestDTO.getAmount());
        transactionDTO.setTransactionType(TransactionType.DEBIT);
        switch (transferOrRequestDTO.getMode()) {
            case "account":
                if (!accountService.getDefaultAccount(user).equals(transferOrRequestDTO.getToAccount())){
                    if(!transferOrRequestDTO.getType().equals("REQUEST"))
                        transactionDTO.setToAccount(transferOrRequestDTO.getToAccount());}
                else{
                    log.error(Instant.now() + ": You can not Transfer Money to same Account via account transfer");
                    throw new GenericRuntimeException("Transfer money can't happen on same account bud ¯\\_(ツ)_/¯");
                }
                break;
            case "email":
                if (!user.getEmail().equals(transferOrRequestDTO.getEmail())) {
                    User toUserEmail = this.getUserByEmail(transferOrRequestDTO.getEmail());
                    transactionDTO.setToAccount(accountService.getDefaultAccount(toUserEmail).getId());
                } else {
                    log.error(Instant.now() + ": You can not Transfer Money to same Account via email");
                    throw new GenericRuntimeException("No way that is going to work ¯\\_(ツ)_/¯");
                }
                break;
            case "phoneNumber":
                if (!user.getPhoneNumber().equals(transferOrRequestDTO.getPhoneNumber())) {
                    User toUserPhone = this.getUserByPhoneNumber(transferOrRequestDTO.getPhoneNumber());
                    transactionDTO.setToAccount(accountService.getDefaultAccount(toUserPhone).getId());
                } else {
                    log.error(Instant.now() + ": You can not Transfer Money to same Account via phoneNumber");
                    throw new GenericRuntimeException("It won't work okay ¯\\_(ツ)_/¯");
                }
                break;
            default:
                log.error(Instant.now() + ": Invalid mode of transfer");
                throw new GenericRuntimeException("GEEK Alert ¯\\_(ツ)_/¯");
        }
        return transactionDTO;
    }
}
