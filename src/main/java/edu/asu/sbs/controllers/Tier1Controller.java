package edu.asu.sbs.controllers;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.jknack.handlebars.Template;
import edu.asu.sbs.config.*;
import edu.asu.sbs.errors.UnauthorizedAccessExcpetion;
import edu.asu.sbs.loader.HandlebarsTemplateLoader;
import edu.asu.sbs.models.Request;
import edu.asu.sbs.models.User;
import edu.asu.sbs.services.*;
import edu.asu.sbs.services.dto.ChequeDTO;
import edu.asu.sbs.services.dto.RequestDTO;
import edu.asu.sbs.services.dto.TransactionDTO;
import edu.asu.sbs.services.dto.ViewAccountDTO;
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
@PreAuthorize("hasAnyAuthority('" + UserType.EMPLOYEE_ROLE1 + "')")
@RestController
@RequestMapping("/api/v1/tier1")
public class Tier1Controller {

    private final AccountService accountService;
    private final HandlebarsTemplateLoader handlebarsTemplateLoader;
    private final TransactionService transactionService;
    private final UserService userService;
    private final RequestService requestService;
    private final TransactionHyperledgerService transactionHyperledgerService;

    ObjectMapper mapper = new ObjectMapper();

    public Tier1Controller(AccountService accountService, HandlebarsTemplateLoader handlebarsTemplateLoader, TransactionService transactionService, UserService userService, RequestService requestService, TransactionHyperledgerService transactionHyperledgerService) {
        this.accountService = accountService;
        this.handlebarsTemplateLoader = handlebarsTemplateLoader;
        this.transactionService = transactionService;
        this.userService = userService;
        this.requestService = requestService;
        this.transactionHyperledgerService = transactionHyperledgerService;
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
        List<ViewAccountDTO> accounts = accountService.getAccounts();
        HashMap<String, List<ViewAccountDTO>> resultMap = new HashMap<>();
        resultMap.put("result", accounts);
        JsonNode result = mapper.valueToTree(resultMap);
        Template template = handlebarsTemplateLoader.getTemplate("tier1UserAccounts");
        return template.apply(handlebarsTemplateLoader.getContext(result));
    }

    @GetMapping("/cheques")
    @ResponseBody
    public String getCheques() throws IOException {
        List<ChequeDTO> chequeDTOList = transactionService.getCheques();
        log.info(chequeDTOList.toString());
        HashMap<String, List<ChequeDTO>> resultMap = new HashMap<>();
        resultMap.put("result", chequeDTOList);
        JsonNode result = mapper.valueToTree(resultMap);
        Template template = handlebarsTemplateLoader.getTemplate("tier1ViewCheques");
        return template.apply(handlebarsTemplateLoader.getContext(result));
    }

    @GetMapping("/viewHyperledgerTransactions")
    @ResponseBody
    public String viewHyperledgerTransactions() {
        return transactionHyperledgerService.getAll();
    }

    @GetMapping("/getHistory/{id}")
    @ResponseBody
    public String getHistoryFromHyperledger(@PathVariable String id) {
        return transactionHyperledgerService.getHistory(id);
    }

    @GetMapping("/getRecord/{id}")
    @ResponseBody
    public String getRecord(@PathVariable String id) {
        return transactionHyperledgerService.getById(Long.valueOf(id));
    }

    @GetMapping("/transactions")
    @ResponseBody
    public String viewTransactions() throws IOException {
        List<TransactionDTO> transactions = transactionService.getTransactions();
        HashMap<String, List<TransactionDTO>> resultMap = new HashMap<>();
        resultMap.put("result", transactions);
        JavaTimeModule module = new JavaTimeModule();
        mapper.registerModule(module);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        JsonNode result = mapper.valueToTree(resultMap);
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

    @PostMapping("/clearCheque")
    @ResponseBody
    public String clearCheque(Long chequeId) {
        return transactionService.clearCheque(chequeId);
    }

    @PostMapping("/denyCheque")
    @ResponseBody
    public void denyCheck(Long chequeId) {
        transactionService.denyCheque(chequeId);
    }

    @PostMapping("/raiseRequest")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void raiseRequest(@RequestBody RequestDTO requestDTO) {

    }

    @GetMapping("/profileUpdateRequests")
    public String getEmpProfileUpdaterRequest() throws IOException {
        ArrayList<Request> allRequests = (ArrayList<Request>) requestService.getUserProfileUpdateRequests();
        HashMap<String, ArrayList<Request>> resultMap = new HashMap<>();
        resultMap.put("result", allRequests);
        JsonNode result = mapper.valueToTree(resultMap);
        Template template = handlebarsTemplateLoader.getTemplate("profileUpdateRequests");
        return template.apply(handlebarsTemplateLoader.getContext(result));
    }

    @PostMapping("/approveUpdateUserProfile/")
    @ResponseStatus(HttpStatus.ACCEPTED)
    private void approveUserProfile(Long requestId, RequestDTO requestDTO) {
        Optional<Request> request = requestService.getRequest(requestId);
        User user = userService.getCurrentUser();
        request.ifPresent(req -> {
            if (RequestType.UPDATE_USER_PROFILE.equals(req.getRequestType()) && req.getStatus().equals(StatusType.PENDING)) {
                requestService.updateUserProfile(req, user, RequestType.UPDATE_USER_PROFILE, StatusType.APPROVED, requestDTO);
            }
        });
    }

    @PostMapping("/declineUpdateUserProfile/")
    private void declineUsereProfile(Long requestId, RequestDTO requestDTO) {
        Optional<Request> request = requestService.getRequest(requestId);
        User user = userService.getCurrentUser();
        request.ifPresent(req -> {
            if (RequestType.UPDATE_USER_PROFILE.equals(req.getRequestType()) && req.getStatus().equals(StatusType.PENDING)) {
                requestService.updateUserProfile(req, user, RequestType.UPDATE_USER_PROFILE, StatusType.DECLINED, requestDTO);
            }
        });
    }
}
