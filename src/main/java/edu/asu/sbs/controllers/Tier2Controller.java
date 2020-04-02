package edu.asu.sbs.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.jknack.handlebars.Template;
import com.google.common.collect.Maps;
import edu.asu.sbs.config.RequestType;
import edu.asu.sbs.config.StatusType;
import edu.asu.sbs.config.UserType;
import edu.asu.sbs.errors.Exceptions;
import edu.asu.sbs.errors.UnauthorizedAccessExcpetion;
import edu.asu.sbs.globals.AccountType;
import edu.asu.sbs.loader.HandlebarsTemplateLoader;
import edu.asu.sbs.models.Request;
import edu.asu.sbs.models.User;
import edu.asu.sbs.services.AccountService;
import edu.asu.sbs.services.RequestService;
import edu.asu.sbs.services.TransactionService;
import edu.asu.sbs.services.UserService;
import edu.asu.sbs.services.dto.ProfileRequestDTO;
import edu.asu.sbs.services.dto.Tier2RequestsDTO;
import edu.asu.sbs.services.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Slf4j
@PreAuthorize("hasAnyAuthority('" + UserType.EMPLOYEE_ROLE2 + "')")
@RestController
@RequestMapping("/api/v1/tier2")
public class Tier2Controller {

    private final UserService userService;
    private final RequestService requestService;
    private final AccountService accountService;
    ObjectMapper mapper = new ObjectMapper();

    //@Autowired
    private final HandlebarsTemplateLoader handlebarsTemplateLoader;

    private final TransactionService transactionService;

    public Tier2Controller(UserService userService, TransactionService transactionService, RequestService requestService, AccountService accountService, HandlebarsTemplateLoader handlebarsTemplateLoader) {
        this.userService = userService;
        this.transactionService = transactionService;
        this.requestService = requestService;
        this.accountService = accountService;
        this.handlebarsTemplateLoader = handlebarsTemplateLoader;
    }

    @GetMapping("/user/add")
    public String getLoginTemplate() throws IOException {
        Template template = handlebarsTemplateLoader.getTemplate("tier2AddNewUser");
        return template.apply("");
    }

    @PostMapping("/user/add")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void signupSubmit(UserDTO newUserRequest, String password, String userType, HttpServletResponse response) throws IOException {
        userService.registerUser(newUserRequest, password, userType);
        log.info("POST request: tier2 new user request");
        response.sendRedirect("/api/v1/tier2/allUsers");
    }

    @GetMapping("/details")
    @ResponseBody
    public String currentUserDetails() throws UnauthorizedAccessExcpetion, IOException {

        User user = userService.getCurrentUser();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        mapper.setDateFormat(df);
        if (user == null) {
            log.info("GET request: Unauthorized request for tier2 employee user detail");
            throw new UnauthorizedAccessExcpetion("401", "Unauthorized Access !");
        }
        JsonNode result = mapper.valueToTree(user);
        Template template = handlebarsTemplateLoader.getTemplate("profileTier2");
        log.info("GET request: Tier2 Employee user detail");
        return template.apply(handlebarsTemplateLoader.getContext(result));
    }

    @GetMapping("/allUsers")
    public String getUsers() throws IOException {
        ArrayList<User> allUsers = (ArrayList<User>) userService.getAllUsers();
        HashMap<String, ArrayList<User>> resultMap = new HashMap<>();
        resultMap.put("result", allUsers);
        JsonNode result = mapper.valueToTree(resultMap);
        Template template = handlebarsTemplateLoader.getTemplate("tier2UserAccess");
        return template.apply(handlebarsTemplateLoader.getContext(result));
    }

    @GetMapping("/{userName}/accounts")
    public String getUsersAccounts(@PathVariable String userName) throws IOException {
        List allAccounts = accountService.getAccountsForUser(userService.getUserByUserName(userName));
        HashMap<String, List> resultMap = new HashMap<>();
        resultMap.put("result", allAccounts);
        JsonNode result = mapper.valueToTree(resultMap);
        System.out.println(result);
        Template template = handlebarsTemplateLoader.getTemplate("tier2UserAccounts");
        return template.apply(handlebarsTemplateLoader.getContext(result));
    }

    @GetMapping("/delete/{id}")
    public void deleteUser(@PathVariable Long id, HttpServletResponse response) throws Exceptions, IOException {
        User current = userService.getUserByIdAndActive(id);
        if (current == null) {
            throw new Exceptions("404", " ");
        }
        if (!(current.getUserType().equals(UserType.USER_ROLE))) {
            log.warn("GET request: tier2 employee unauthorised request access");
            throw new Exceptions("401", "Unauthorized request !!");
        }

        userService.deleteUser(id);
        log.info("POST request: Delete User request");
        response.sendRedirect("/api/v1/tier2/allUsers");
    }

    @GetMapping("/viewUser/{id}")
    public String getUserDetail(@PathVariable Long id) throws Exceptions, IOException {
        User user = userService.getUserByIdAndActive(id);

        if (user == null) {
            throw new Exceptions("404", " ");
        }
        if (!(user.getUserType().equals(UserType.USER_ROLE))) {
            log.warn("GET request: Unauthorized request for external user");
            throw new Exceptions("409", " ");
        }

        JsonNode result = mapper.valueToTree(user);
        Template template = handlebarsTemplateLoader.getTemplate("tier2ViewUser");
        return template.apply(handlebarsTemplateLoader.getContext(result));
    }

    @GetMapping("/transactions")
    public String getAllUserRequest() throws IOException {
        List<Tier2RequestsDTO> allRequests = requestService.getAllTier2Requests();
        HashMap<String, List<Tier2RequestsDTO>> resultMap = Maps.newHashMap();
        resultMap.put("result", allRequests);
        JavaTimeModule module = new JavaTimeModule();
        mapper.registerModule(module);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        JsonNode result = mapper.valueToTree(resultMap);
        Template template = handlebarsTemplateLoader.getTemplate("tier2Transactions");
        return template.apply(handlebarsTemplateLoader.getContext(result));
    }

    @PostMapping("/approveTransaction")
    public void approveEdit(Long id, HttpServletResponse response) throws IOException {

        Optional<Request> request = requestService.getRequest(id);
        User user = userService.getCurrentUser();
        request.ifPresent(req -> {
            if (RequestType.APPROVE_CRITICAL_TRANSACTION.equals(req.getRequestType()) && req.getStatus().equals(StatusType.PENDING)) {
                requestService.modifyRequest(req, user, RequestType.APPROVE_CRITICAL_TRANSACTION, StatusType.APPROVED);
            }
        });
        response.sendRedirect("transactions");
    }

    @PostMapping("/denyTransaction")
    public void denyTransaction(Long id, HttpServletResponse response) throws IOException {

        Optional<Request> request = requestService.getRequest(id);
        User user = userService.getCurrentUser();
        request.ifPresent(req -> {
            if (RequestType.APPROVE_CRITICAL_TRANSACTION.equals(req.getRequestType()) && req.getStatus().equals(StatusType.PENDING)) {
                requestService.modifyRequest(req, user, RequestType.APPROVE_CRITICAL_TRANSACTION, StatusType.DECLINED);
            }
        });
        response.sendRedirect("transactions");
    }

    @PostMapping("/modifyAccount")
    public void modifyUserAccount(Long id, AccountType accountType, HttpServletResponse response) throws IllegalStateException, IOException {

        switch (accountType) {
            case CHECKING:
                accountService.updateAccountType(id, AccountType.CHECKING);
                break;
            case SAVINGS:
                accountService.updateAccountType(id, AccountType.SAVINGS);
                break;
            case CURRENT:
                accountService.updateAccountType(id, AccountType.CURRENT);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + accountType);
        }
        response.sendRedirect("transactions");
    }

    @PostMapping("/closeAccount")
    public void closeAccount(Long id, HttpServletResponse response) throws IllegalStateException, IOException {
        if (id != null) {
            accountService.closeUserAccount(id);
        } else {
            throw new IllegalStateException("Incorrect Id: " + id);
        }
        response.sendRedirect("transactions");
    }

    @PostMapping("/approveCreateAccountReq")
    public void approveAdditionalAccount(Long id, HttpServletResponse response) throws IOException {

        Optional<Request> request = requestService.getRequest(id);
        User user = userService.getCurrentUser();
        request.ifPresent(req -> {
            if (RequestType.CREATE_ADDITIONAL_ACCOUNT.equals(req.getRequestType()) && req.getStatus().equals(StatusType.PENDING)) {
                requestService.updateAccountCreationRequest(req, user, RequestType.CREATE_ADDITIONAL_ACCOUNT, StatusType.APPROVED);
            }
        });
        response.sendRedirect("transactions");
    }

    @PostMapping("/denyCreateAccountReq")
    public void declineAdditionalAccount(Long id, HttpServletResponse response) throws IOException {

        Optional<Request> request = requestService.getRequest(id);
        User user = userService.getCurrentUser();
        request.ifPresent(req -> {
            if (RequestType.CREATE_ADDITIONAL_ACCOUNT.equals(req.getRequestType()) && req.getStatus().equals(StatusType.PENDING)) {
                requestService.updateAccountCreationRequest(req, user, RequestType.CREATE_ADDITIONAL_ACCOUNT, StatusType.DECLINED);
            }
        });
        response.sendRedirect("transactions");
    }

    @PostMapping("/raiseProfileUpdateRequest")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void raiseProfileUpdateRequest(ProfileRequestDTO requestDTO, HttpServletResponse response) throws IOException {

        if (userService.getCurrentUser().getUserType().equals(UserType.EMPLOYEE_ROLE2)) {
            requestService.createProfileUpdateRequest(requestDTO, RequestType.UPDATE_EMP_PROFILE);
        }
        response.sendRedirect("transactions");
    }

    @GetMapping("/raiseChangeRoleRequest")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void createChangeRoleRequestPost(HttpServletResponse response) throws IOException {
        if (userService.getCurrentUser().getUserType().equals(UserType.EMPLOYEE_ROLE2)) {
            requestService.createChangeRoleRequest(RequestType.TIER2_TO_TIER1);
        }
        response.sendRedirect("transactions");
    }
}