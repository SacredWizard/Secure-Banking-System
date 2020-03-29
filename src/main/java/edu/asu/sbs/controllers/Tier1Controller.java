package edu.asu.sbs.controllers;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Template;
import edu.asu.sbs.config.*;
import edu.asu.sbs.errors.UnauthorizedAccessExcpetion;
import edu.asu.sbs.loader.HandlebarsTemplateLoader;
import edu.asu.sbs.models.Request;
import edu.asu.sbs.models.User;
import edu.asu.sbs.services.AccountService;
import edu.asu.sbs.services.RequestService;
import edu.asu.sbs.services.TransactionService;
import edu.asu.sbs.services.UserService;
import edu.asu.sbs.services.dto.AccountDTO;
import edu.asu.sbs.services.dto.RequestDTO;
import edu.asu.sbs.services.dto.TransactionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Slf4j
@PreAuthorize("hasAnyAuthority('" + UserType.EMPLOYEE_ROLE1 + "')")
@RestController
@RequestMapping("/api/v1/tier1")
public class Tier1Controller {

    private final AccountService accountService;
    private final HandlebarsTemplateLoader handlebarsTemplateLoader;
    private final TransactionService transactionService;
    private final UserService userService;
    private final RequestService requestService;

    ObjectMapper mapper = new ObjectMapper();

    public Tier1Controller(AccountService accountService, HandlebarsTemplateLoader handlebarsTemplateLoader, TransactionService transactionService, UserService userService, RequestService requestService) {
        this.accountService = accountService;
        this.handlebarsTemplateLoader = handlebarsTemplateLoader;
        this.transactionService = transactionService;
        this.userService = userService;
        this.requestService = requestService;
    }

    @GetMapping("/profile")
    @ResponseBody
    public String getProfileTemplate() throws IOException, UnauthorizedAccessExcpetion {
        User user = userService.getCurrentUser();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        mapper.setDateFormat(df);
        if (user == null) {
            log.info("GET request: Unauthorized request for tier1 user detail");
            throw new UnauthorizedAccessExcpetion("401", "Unauthorized Access !");
        }

        JsonNode result = mapper.valueToTree(user);
        Template template = handlebarsTemplateLoader.getTemplate("profileTier1");
        log.info("GET request: Admin user detail");
        return template.apply(handlebarsTemplateLoader.getContext(result));
    }

    @GetMapping("/accounts")
    @ResponseBody
    public String getAccounts() throws IOException {
        List accounts = accountService.getAccounts();
        HashMap<String, List> resultMap = new HashMap<>();
        resultMap.put("result", accounts);
        JsonNode result = mapper.valueToTree(resultMap);
        System.out.println(result);
        Template template = handlebarsTemplateLoader.getTemplate("tier1UserAccounts");
        return template.apply(handlebarsTemplateLoader.getContext(result));
    }

    @GetMapping("/transactions")
    @ResponseBody
    public String viewTransactions() throws IOException {
        List transactions = transactionService.getTransactions();
        HashMap<String, List> resultMap = new HashMap<>();
        resultMap.put("result", transactions);
        JsonNode result = mapper.valueToTree(resultMap);
        System.out.println(result);
        Template template = handlebarsTemplateLoader.getTemplate("tier1TransactionRequests");
        return template.apply(handlebarsTemplateLoader.getContext(result));
    }

    @GetMapping("/createTransaction")
    @ResponseBody
    public String getCreateTransactionTemplate() throws IOException {
        Template template = handlebarsTemplateLoader.getTemplate("tier1CreateTransactions");
        return template.apply("");
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void createTransaction(TransactionDTO transactionDTO, HttpServletResponse response) throws IOException {
        if (transactionDTO.getTransactionType().equals(TransactionType.CHEQUE)) {
            transactionService.issueCheque(transactionDTO);
        } else {
            transactionService.createTransaction(transactionDTO, TransactionStatus.APPROVED);
        }
        response.sendRedirect("transactions");
    }

    @PostMapping("/issueCheck")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void issueCheque(@RequestBody TransactionDTO transactionDTO) {
        transactionService.issueCheque(transactionDTO);
    }

    @PutMapping("/clearCheck")
    @ResponseBody
    public String clearCheck(@RequestParam Long chequeId) {
        return transactionService.clearCheque(chequeId);
    }

    @PostMapping("/raiseRequest")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void raiseRequest(@RequestBody RequestDTO requestDTO) {

    }

    @PostMapping("/approveNewAccountReq")
    public void approveEdit(Long id, AccountDTO accountDTO, HttpServletResponse response) throws IOException {

        Optional<Request> request = requestService.getRequest(id);
        User user = userService.getCurrentUser();
        request.ifPresent(req -> {
            if (RequestType.CREATE_NEW_ACCOUNT.equals(req.getRequestType()) && req.getStatus().equals(StatusType.PENDING)) {
                requestService.updateAccountCreationRequest(req, user, RequestType.CREATE_NEW_ACCOUNT, StatusType.APPROVED, accountDTO);
            }
        });
        response.sendRedirect("transactions");
    }

    @PostMapping("/denyNewAccountReq")
    public void denyTransaction(Long id, AccountDTO accountDTO, HttpServletResponse response) throws IOException {

        Optional<Request> request = requestService.getRequest(id);
        User user = userService.getCurrentUser();
        request.ifPresent(req -> {
            if (RequestType.CREATE_NEW_ACCOUNT.equals(req.getRequestType()) && req.getStatus().equals(StatusType.PENDING)) {
                requestService.updateAccountCreationRequest(req, user, RequestType.CREATE_NEW_ACCOUNT, StatusType.DECLINED, accountDTO);
            }
        });
        response.sendRedirect("transactions");
    }

    @PostMapping("/approveUpdateUserProfile/")
    private void approveUserProfile(Long requestId, RequestDTO requestDTO) {
        Optional<Request> request = requestService.getRequest(requestId);
        User user = userService.getCurrentUser();
        request.ifPresent(req -> {
            if (RequestType.UPDATE_PROFILE.equals(req.getRequestType()) && req.getStatus().equals(StatusType.PENDING)) {
                requestService.updateUserProfile(req, user, RequestType.UPDATE_PROFILE, StatusType.APPROVED, requestDTO);
            }
        });
    }

    @PostMapping("/declineUpdateUserProfile/")
    private void declineUsereProfile(Long requestId, RequestDTO requestDTO) {
        Optional<Request> request = requestService.getRequest(requestId);
        User user = userService.getCurrentUser();
        request.ifPresent(req -> {
            if (RequestType.UPDATE_PROFILE.equals(req.getRequestType()) && req.getStatus().equals(StatusType.PENDING)) {
                requestService.updateUserProfile(req, user, RequestType.UPDATE_PROFILE, StatusType.DECLINED, requestDTO);
            }
        });
    }
}
