package edu.asu.sbs.controllers;

import com.github.jknack.handlebars.Template;
import edu.asu.sbs.config.Constants;
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
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
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

    @PostMapping("/signup_test")
    public void testing(@RequestBody HttpServletRequest payload) {
        log.info(payload.toString());
    }

    @PostMapping(path = "/authenticate", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<UserService.JWTToken> authenticate(LoginVM loginVM) {
        return userService.authenticate(loginVM);
    }

    @PostMapping(path = "/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerUser(@RequestBody ManageUserVM manageUserVM) {
        log.info(manageUserVM.toString());
        if (checkPasswordLength(manageUserVM.getPassword())) {
            throw new InvalidPasswordException();
        }
        User user = userService.registerUser(manageUserVM, manageUserVM.getPassword());
        mailService.sendActivationEmail(user);

    }

    @GetMapping("/activate")
    public void activate(@RequestParam(value = "key") String key) {
        Optional<User> user = userService.activateRegistration(key);
        if (!user.isPresent()) {
            throw new AccountResourceException("No user was found for this activation key");
        }
    }

    @PostMapping(path = "/reset-password/init")
    public void requestPasswordReset(@RequestBody String email) {
        Optional<User> user = userService.requestPasswordReset(email);
        if (user.isPresent()) {
            mailService.sendPasswordResetMail(user.get());
        } else {
            // Pretend the request has been successful to prevent checking which emails really exist
            // but log that an invalid attempt has been made
            log.warn("Password reset requested for non existing mail '{}'", email);
        }
    }

    @PostMapping(path = "/reset-password/finish")
    public void finishPasswordReset(@RequestBody KeyAndPasswordVM keyAndPassword) {
        if (checkPasswordLength(keyAndPassword.getNewPassword())) {
            throw new InvalidPasswordException();
        }
        Optional<User> user =
                userService.completePasswordReset(keyAndPassword.getNewPassword(), keyAndPassword.getKey());

        if (!user.isPresent()) {
            throw new AccountResourceException("No user was found for this reset key");
        }
    }

    @GetMapping("/logout")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        return userService.logout(request, response);
    }

    @GetMapping("/test")
    @ResponseBody
    public JSONObject test() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("ASUH", "ASDJH");
        jsonObject.put("Array", new JSONArray().put(1));
        return jsonObject;
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
