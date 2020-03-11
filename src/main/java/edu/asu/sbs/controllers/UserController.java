package edu.asu.sbs.controllers;

import edu.asu.sbs.config.Constants;
import edu.asu.sbs.errors.AccountResourceException;
import edu.asu.sbs.errors.InvalidPasswordException;
import edu.asu.sbs.models.User;
import edu.asu.sbs.services.UserService;
import edu.asu.sbs.vm.LoginVM;
import edu.asu.sbs.vm.ManageUserVM;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;


    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/authenticate")
    public ResponseEntity<UserService.JWTToken> authenticate(@Valid @RequestBody LoginVM loginVM) throws JSONException {
        return userService.authenticate(loginVM);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerUser(@Valid @RequestBody ManageUserVM manageUserVM) {
        if (!checkPasswordLength(manageUserVM.getPassword())) {
            throw new InvalidPasswordException();
        }
        userService.registerUser(manageUserVM, manageUserVM.getPassword());
    }

    @GetMapping("/activate")
    public void activate(@RequestParam(value = "key") String key) {
        Optional<User> user = userService.activateRegistration(key);
        if (!user.isPresent()) {
            throw new AccountResourceException("No user was found for this activation key");
        }
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
        return !StringUtils.isEmpty(password) &&
                password.length() >= Constants.PASSWORD_MIN_LENGTH &&
                password.length() <= Constants.PASSWORD_MAX_LENGTH;
    }
}
