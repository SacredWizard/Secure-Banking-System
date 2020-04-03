package edu.asu.sbs.controllers;

import com.github.jknack.handlebars.Template;
import edu.asu.sbs.config.Constants;
import edu.asu.sbs.config.UserType;
import edu.asu.sbs.errors.AccountResourceException;
import edu.asu.sbs.errors.InvalidPasswordException;
import edu.asu.sbs.loader.HandlebarsTemplateLoader;
import edu.asu.sbs.models.User;
import edu.asu.sbs.services.MailService;
import edu.asu.sbs.services.UserService;
import edu.asu.sbs.vm.KeyAndPasswordVM;
import edu.asu.sbs.vm.LoginVM;
import edu.asu.sbs.vm.ManageUserVM;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/user")
@Slf4j
public class UserController {

    private final UserService userService;
    private final HandlebarsTemplateLoader handlebarsTemplateLoader;
    private final MailService mailService;

    public UserController(UserService userService, HandlebarsTemplateLoader handlebarsTemplateLoader, MailService mailService) {
        this.userService = userService;
        this.handlebarsTemplateLoader = handlebarsTemplateLoader;
        this.mailService = mailService;
    }


    @GetMapping(value = "/signup", produces = "text/html")
    public String getHomeTemplate() throws IOException {
        Template template = handlebarsTemplateLoader.getTemplate("signup");
        return template.apply("");
    }

    @GetMapping(value = "/login", produces = "text/html")
    public String getLoginTemplate() throws IOException {
        Template template = handlebarsTemplateLoader.getTemplate("login");
        return template.apply("");
    }

    @PostMapping(path = "/authenticate", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<UserService.JWTToken> authenticate(LoginVM loginVM, HttpServletResponse response) throws IOException {
        userService.authenticate(loginVM);
        User user = userService.getCurrentUser();
        if (user.getUserType().equals(UserType.ADMIN_ROLE))
            response.sendRedirect("/api/v1/admin/allEmployees");
        else if (user.getUserType().equals((UserType.USER_ROLE)))
            response.sendRedirect("/api/v1/customer/home");
        else if (user.getUserType().equals((UserType.EMPLOYEE_ROLE1)))
            response.sendRedirect("/api/v1/tier1/home");
        else if (user.getUserType().equals((UserType.EMPLOYEE_ROLE2)))
            response.sendRedirect("/api/v1/tier1/home");
        return userService.authenticate(loginVM);
    }

    @PostMapping(path = "/register")
    @ResponseStatus(HttpStatus.CREATED)
    public String registerUser(ManageUserVM manageUserVM, String userType) throws IOException {
        log.info(manageUserVM.toString());
        if (checkPasswordLength(manageUserVM.getPassword())) {
            throw new InvalidPasswordException();
        }
        User user = userService.registerUser(manageUserVM, manageUserVM.getPassword(), userType);
        mailService.sendActivationEmail(user);
        Template template = handlebarsTemplateLoader.getTemplate("activate");
        return template.apply("");
    }


    @GetMapping("/activate")
    public void activate(@RequestParam(value = "key") String key, HttpServletResponse response) throws IOException {
        Optional<User> user = userService.activateRegistration(key);
        if (!user.isPresent()) {
            throw new AccountResourceException("LOL! No user was found for this activation key ¯\\_(ツ)_/¯");
        }
        response.sendRedirect("/api/v1/user/login");
    }

    @GetMapping(value = "/reset-password/init", produces = "text/html")
    public String getResetPasswordTemplate() throws IOException {
        Template template = handlebarsTemplateLoader.getTemplate("forgotPasswordInit");
        return template.apply("");
    }


    @PostMapping(path = "/reset-password/init")
    public void requestPasswordReset(String email, HttpServletResponse response) throws IOException {
        Optional<User> user = userService.requestPasswordReset(email);
        if (user.isPresent()) {
            mailService.sendPasswordResetMail(user.get());
        } else {
            // Pretend the request has been successful to prevent checking which emails really exist
            // but log that an invalid attempt has been made
            log.warn("Password reset requested for non existing mail '{}'", email);
        }
        response.sendRedirect("/reset-password/finish");
    }

    @GetMapping(value = "/reset-password/finish", produces = "text/html")
    public String getResetPasswordFinishTemplate() throws IOException {
        Template template = handlebarsTemplateLoader.getTemplate("forgotPasswordFinish");
        return template.apply("");
    }

    @PostMapping(path = "/reset-password/finish")
    public void finishPasswordReset(KeyAndPasswordVM keyAndPassword, HttpServletResponse response) throws IOException {
        if (checkPasswordLength(keyAndPassword.getNewPassword())) {
            throw new InvalidPasswordException();
        }
        Optional<User> user =
                userService.completePasswordReset(keyAndPassword.getNewPassword(), keyAndPassword.getKey());

        if (!user.isPresent()) {
            throw new AccountResourceException("No sniffing around, No user was found for this reset key ¯\\_(ツ)_/¯");
        }
        response.sendRedirect("/api/v1/user/login");
    }

    @GetMapping("/logout")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        return userService.logout(request, response);
    }

    @GetMapping("/test")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void test() {
        userService.createUpdateUser();
    }


    private static boolean checkPasswordLength(String password) {
        log.info(String.valueOf(StringUtils.isEmpty(password) ||
                password.length() < Constants.PASSWORD_MIN_LENGTH ||
                password.length() > Constants.PASSWORD_MAX_LENGTH));
        return StringUtils.isEmpty(password) ||
                password.length() < Constants.PASSWORD_MIN_LENGTH ||
                password.length() > Constants.PASSWORD_MAX_LENGTH;
    }
}
