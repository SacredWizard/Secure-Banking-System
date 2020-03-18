package edu.asu.sbs.controllers;

import com.google.gson.Gson;
import edu.asu.sbs.config.RequestType;
import edu.asu.sbs.config.UserType;
import edu.asu.sbs.errors.Exceptions;
import edu.asu.sbs.errors.UnauthorizedAccessExcpetion;
import edu.asu.sbs.models.Request;
import edu.asu.sbs.models.User;
import edu.asu.sbs.services.RequestService;
import edu.asu.sbs.services.UserService;
import edu.asu.sbs.services.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

@Slf4j
@PreAuthorize("hasAuthority('" + UserType.ADMIN_ROLE + "')")
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController{

    private final UserService userService;
    private final RequestService requestService;

    public AdminController(UserService userService, RequestService requestService) {
        this.userService = userService;
        this.requestService = requestService;
    }

    @GetMapping("/employee/details")
    @ResponseBody
    public JSONObject currentUserDetails() throws UnauthorizedAccessExcpetion, JSONException {

        User user = userService.getCurrentUser();

        if (user == null) {
            log.info("GET request: Unauthorized request for admin user detail");
            throw new UnauthorizedAccessExcpetion("401", "Unauthorized Access !");
        }

        log.info("GET request: Admin user detail");
        return new JSONObject(new Gson().toJson(user));
    }

    @PostMapping("/employee/add")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void signupSubmit(UserDTO newUserRequest, String password, String userType) throws Exceptions {
        userService.registerUser(newUserRequest, password, userType);
        log.info("POST request: Admin new user request");
    }

    @GetMapping("/allEmployees")
    public JSONArray getUsers() throws Exceptions, JSONException {
        return new JSONArray(new Gson().toJson(userService.getAllEmployees()));
    }

    @PostMapping("/delete/{id}")
    public void deleteUser(@PathVariable Long id) throws Exceptions {
        Optional<User> current = userService.getUserByIdAndActive(id);
        if (current == null) {
            throw new Exceptions("404", " ");
        }
        if (!current.get().getUserType().equals("EMPLOYEE_ROLE1")) {
            log.warn("GET request: Admin unauthrorised request access");
            throw new Exceptions("401", "Unauthorized request !!");
        }

        userService.deleteUser(id);
        log.info("POST request: Employee New modification request");
    }


    @GetMapping("/viewEmployee/{id}")
    public JSONObject getUserDetail(@PathVariable Long id) throws Exceptions, JSONException {
        Optional<User> user = userService.getUserByIdAndActive(id);
        if (user == null) {
            throw new Exceptions("404", " ");
        }
        if (!user.get().getUserType().equals("EMPLOYEE_ROLE1")) {
            log.warn("GET request: Unauthorized request for external user");
            throw new Exceptions("409", " ");
        }

        log.info("GET request: Internal user details by id");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user", user);
        return jsonObject;
    }

    @GetMapping("/requests")
    public JSONArray getAllUserRequest() throws JSONException {
        return new JSONArray(new Gson().toJson(requestService.getAllRequests()));
    }

    @PutMapping("/requests/approve/{id}")
    public void approveEdit(@PathVariable Long id) throws Exceptions {

        Optional<Request> request = requestService.getRequest(id);
        request.ifPresent(req -> {
            switch (req.getRequestType()) {
                case RequestType.TIER1_TO_TIER2:
                    userService.updateUserType(req.getRequestBy(), UserType.EMPLOYEE_ROLE2);
                    break;
                case RequestType.TIER2_TO_TIER1:
                    userService.updateUserType(req.getRequestBy(), UserType.EMPLOYEE_ROLE1);
                    break;
                case RequestType.UPDATE_PROFILE:
                    break;
            }
        });
    }

    /*
     @GetMapping("/requests/view/{id}")
    public JSONObject getUserRequest(@PathVariable() UUID id) throws Exceptions, JSONException {
        UpdateRequest updateRequest = UpdateRequestService.getUpdateRequest(id);
        JSONObject jsonObject = new JSONObject();
        if (updateRequest == null) {
            throw new Exceptions("404", "Invalid Request !");
        }
        if (!updateRequest.getUserType().equals("internal")) {
            log.warn("GET request: Admin unauthrorised request access");
            throw new Exceptions("401", "Unauthorised Request !");
        }
        jsonObject.put("modificationRequests", updateRequest);
        log.info("GET request: User modification request by ID");

        return jsonObject;
    }
    @GetMapping("/request/delete/{id}")
    public JSONObject getDeleteRequest(@PathVariable() UUID id) throws Exceptions, JSONException {
        UpdateRequest updateRequest = UpdateRequestService.getUpdateRequest(id);

        if (updateRequest == null) {
            throw new Exceptions("404","Invalid Request !");
        }
        if (!updateRequest.getUserType().equals("internal")) {
            log.warn("GET request: Admin unauthrorised request access");
            throw new Exceptions("401","Unauthorised Request !");
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("modificationRequests", updateRequest);
        log.info("GET request: User modification request by ID");

        return jsonObject;
    }

    @PostMapping("/request/delete/{requestId}")
    public void deleteRequest(@PathVariable UUID requestId) throws Exceptions {
        UpdateRequest request = UpdateRequestService.getUpdateRequest(requestId);

        // checks validity of request
        if (request == null) {
            throw new Exceptions("404", "Invalid Request !");
        }

        if (!UpdateRequestService.verifyUpdateRequestUserType(requestId, "internal")) {
            log.warn("GET request: Admin unauthrorised request access");
            throw new Exceptions("401", "Unauthorised Request !");
        }
        UpdateRequestService.deleteUpdateRequest(request);
        log.info("POST request: Admin approves modification request");
    }
     */

    @GetMapping("/logDownload")
    public void doDownload(HttpServletRequest request,
                           HttpServletResponse response) throws IOException {

        /**
         * Size of a byte buffer to read/write file
         */
        final int BUFFER_SIZE = 4096;

        /**
         * Path of the file to be downloaded, relative to application's directory
         */
        String filePath = "logs/application.log";


        // get absolute path of the application
        ServletContext context = request.getServletContext();

        ClassLoader classLoader = getClass().getClassLoader();

        // construct the complete absolute path of the file
        File downloadFile = new File(classLoader.getResource(filePath).getFile());
        FileInputStream inputStream = new FileInputStream(downloadFile);

        // get MIME type of the file
        String mimeType = context.getMimeType("text/plain");
        if (mimeType == null) {
            // set to binary type if MIME mapping not found
            mimeType = "application/octet-stream";
        }
        System.out.println("MIME type: " + mimeType);

        // set content attributes for the response
        response.setContentType(mimeType);
        response.setContentLength((int) downloadFile.length());

        // set headers for the response
        String headerKey = "Content-Disposition";
        String headerValue = String.format("attachment; filename=\"%s\"",
                downloadFile.getName());
        response.setHeader(headerKey, headerValue);

        // get output stream of the response
        OutputStream outStream = response.getOutputStream();

        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead = -1;

        // write bytes read from the input stream into the output stream
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, bytesRead);
        }

        inputStream.close();
        outStream.close();

    }
}