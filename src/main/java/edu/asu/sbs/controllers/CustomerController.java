package edu.asu.sbs.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Template;
import edu.asu.sbs.config.RequestType;
import edu.asu.sbs.config.TransactionStatus;
import edu.asu.sbs.config.TransactionType;
import edu.asu.sbs.config.UserType;
import edu.asu.sbs.errors.GenericRuntimeException;
import edu.asu.sbs.errors.UnauthorizedAccessExcpetion;
import edu.asu.sbs.globals.AccountType;
import edu.asu.sbs.globals.CreditDebitType;
import edu.asu.sbs.loader.HandlebarsTemplateLoader;
import edu.asu.sbs.models.Account;
import edu.asu.sbs.models.User;
import edu.asu.sbs.services.*;
import edu.asu.sbs.services.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/customer")
public class CustomerController {

    private final UserService userService;
    private final RequestService requestService;
    private final AccountService accountService;
    ObjectMapper mapper = new ObjectMapper();
    private final HandlebarsTemplateLoader handlebarsTemplateLoader;
    private final OTPService otpService;
    private final MailService mailService;

    private final TransactionService transactionService;

    public CustomerController(UserService userService, TransactionService transactionService, RequestService requestService, AccountService accountService, HandlebarsTemplateLoader handlebarsTemplateLoader, OTPService otpService, MailService mailService) {
        this.userService = userService;
        this.transactionService = transactionService;
        this.requestService = requestService;
        this.accountService = accountService;
        this.handlebarsTemplateLoader = handlebarsTemplateLoader;
        this.otpService = otpService;
        this.mailService = mailService;
    }

    @PreAuthorize("hasAnyAuthority('" + UserType.USER_ROLE + "," + UserType.MERCHANT_ROLE + "')")
    @GetMapping("/home")
    @ResponseBody
    public String getAccounts() throws IOException {
        User currentUser = userService.getCurrentUser();
        List<Account> allAccounts = accountService.getAccountsForUser(currentUser);
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("result", allAccounts);
        resultMap.put("userType", currentUser.getUserType());
        JsonNode result = mapper.valueToTree(resultMap);
        Template template = handlebarsTemplateLoader.getTemplate("extUserAccountSummary");
        return template.apply(handlebarsTemplateLoader.getContext(result));
    }

    @PreAuthorize("hasAnyAuthority('" + UserType.USER_ROLE + "," + UserType.MERCHANT_ROLE + "')")
    @GetMapping("/profile")
    @ResponseBody
    public String currentUserDetails() throws UnauthorizedAccessExcpetion, IOException {
        User currentUser = userService.getCurrentUser();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        mapper.setDateFormat(df);
        if (currentUser == null) {
            log.info("GET request: Unauthorized request for User detail");
            throw new UnauthorizedAccessExcpetion("401", "Unauthorized Access !");
        }
        JsonNode result = mapper.valueToTree(currentUser);
        Template template = handlebarsTemplateLoader.getTemplate("profileExtUser");
        log.info("GET request: Customer detail");
        return template.apply(handlebarsTemplateLoader.getContext(result));
    }

    @PreAuthorize("hasAnyAuthority('" + UserType.USER_ROLE + "," + UserType.MERCHANT_ROLE + "')")
    @GetMapping("/creditOrDebit")
    @ResponseBody
    public String getCreditOrDebitTemplate() throws IOException {
        Template template = handlebarsTemplateLoader.getTemplate("extUserCreditOrDebit");
        return template.apply("");
    }

    @PreAuthorize("hasAnyAuthority('" + UserType.USER_ROLE + "," + UserType.MERCHANT_ROLE + "')")
    @PostMapping("/creditOrDebit")
    @ResponseBody
    public void creditOrDebit(CreditDebitDTO creditDebitRequest, HttpServletResponse response) throws IOException {
        User currentUser = userService.getCurrentUser();
        try {
            if (creditDebitRequest.getCreditDebitType() == CreditDebitType.CREDIT || creditDebitRequest.getCreditDebitType() == CreditDebitType.DEBIT)
                accountService.makeSelfTransaction(currentUser, creditDebitRequest);
            else
                throw new GenericRuntimeException("Invalid Request, Please Try again");
            log.info(creditDebitRequest.getCreditDebitType() + " Success for account " + creditDebitRequest.getId());
        } catch (Exception e) {
            //redirect error message
            System.out.println(e.getMessage());
        }
        response.sendRedirect("home");
    }

    @PreAuthorize("hasAnyAuthority('" + UserType.USER_ROLE + "')")
    @GetMapping("/requestNewAccount")
    @ResponseBody
    public String getRequestNewAccountTemplate() throws IOException {
        Template template = handlebarsTemplateLoader.getTemplate("extUserRequestNewAccount");
        return template.apply("");
    }

    @PreAuthorize("hasAnyAuthority('" + UserType.USER_ROLE + "')")
    @PostMapping("/requestAccount")
    @ResponseStatus(HttpStatus.CREATED)
    public String createAccount(NewAccountRequestDTO newAccountRequestDTO) throws IOException {
        User currentUser = userService.getCurrentUser();
        NewAccountRequestDTO newAccountResponseDTO = accountService.createAccount(currentUser, newAccountRequestDTO);
        return getAccountRequests();
    }

    @PreAuthorize("hasAnyAuthority('" + UserType.USER_ROLE + "," + UserType.MERCHANT_ROLE + "')")
    @GetMapping("/transferFunds")
    @ResponseBody
    public String getTransferFundsTemplate() throws IOException {
        Template template = handlebarsTemplateLoader.getTemplate("extUserTransferFunds");
        return template.apply("");
    }

    @PreAuthorize("hasAnyAuthority('" + UserType.USER_ROLE + "," + UserType.MERCHANT_ROLE + "')")
    @PostMapping("/transferFunds")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public String createTransaction(TransactionDTO transactionDTO, HttpServletResponse response) throws IOException {
        User user = userService.getCurrentUser();
        Optional<Account> fromAccount = accountService.getAccountById(transactionDTO.getFromAccount());
        if (fromAccount.isPresent()) {
            User fromUser = fromAccount.get().getUser();
            if (user == fromUser) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                otpService.generateOTP(auth).ifPresent(mailService::sendOTPMail);
                JsonNode result = mapper.valueToTree(transactionDTO);
                Template template = handlebarsTemplateLoader.getTemplate("otpTransferFunds");
                return template.apply(handlebarsTemplateLoader.getContext(result));
            } else {
                throw new GenericRuntimeException("Please give your account number");
            }
        }
        throw new GenericRuntimeException("User is not valid");
    }

    @PreAuthorize("hasAnyAuthority('" + UserType.USER_ROLE + "," + UserType.MERCHANT_ROLE + "')")
    @PostMapping("/transferFundsOtp")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void createTransactionOtp(TransactionDTO transactionDTO, Integer otp, HttpServletResponse response) throws IOException {
        if (otpService.validateOtp(otp)) {
            User user = userService.getCurrentUser();
            accountService.getAccountById(transactionDTO.getFromAccount()).ifPresent(fromUser -> {
                if (fromUser.getUser() == user) {
                    transactionDTO.setTransactionType(TransactionType.DEBIT);
                    transactionService.createTransaction(transactionDTO, TransactionStatus.APPROVED);
                } else {
                    throw new GenericRuntimeException("Please give your account number");
                }
            });
        } else {
            throw new GenericRuntimeException("Invalid OTP");
        }

        response.sendRedirect("home");
    }

    @PreAuthorize("hasAnyAuthority('" + UserType.USER_ROLE + "," + UserType.MERCHANT_ROLE + "')")
    @GetMapping("/transferOrRequest")
    @ResponseBody
    public String getTransferOrRequestTemplate() throws IOException {
        Template template = handlebarsTemplateLoader.getTemplate("extUserTransferAndRequestPayments");
        return template.apply("");
    }

    @PreAuthorize("hasAnyAuthority('" + UserType.USER_ROLE + "," + UserType.MERCHANT_ROLE + "')")
    @PostMapping("/transferOrRequest")
    @ResponseStatus(HttpStatus.OK)
    public String transferOrRequest(TransferOrRequestDTO transferOrRequestDTO, HttpServletResponse response) throws IOException, NullPointerException {
        switch (transferOrRequestDTO.getType()) {
            case "TRANSFER":
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                otpService.generateOTP(auth).ifPresent(mailService::sendOTPMail);
                JsonNode result = mapper.valueToTree(transferOrRequestDTO);
                Template template = handlebarsTemplateLoader.getTemplate("otpTransaction");
                return template.apply(handlebarsTemplateLoader.getContext(result));
            case "REQUEST":
                break;
            default:
                throw new GenericRuntimeException("Invalid Type of request");
        }
        response.sendRedirect("home");
        return null;
    }

    @PreAuthorize("hasAnyAuthority('" + UserType.USER_ROLE + "," + UserType.MERCHANT_ROLE + "')")
    @PostMapping("/transferOrRequestOtp")
    @ResponseStatus(HttpStatus.OK)
    public void transferOrRequestOtp(TransferOrRequestDTO transferOrRequestDTO, Integer otp, HttpServletResponse response) throws IOException, NullPointerException {

        switch (transferOrRequestDTO.getType()) {
            case "TRANSFER":
                if (otpService.validateOtp(otp)) {
                    TransactionDTO transactionDTO = userService.transferByEmailOrPhone(transferOrRequestDTO);
                    transactionService.createTransaction(transactionDTO, TransactionStatus.APPROVED);
                }
                break;
            case "REQUEST":
                break;
            default:
                throw new GenericRuntimeException("Invalid Type of request");
        }
        response.sendRedirect("home");
    }

    @PreAuthorize("hasAnyAuthority('" + UserType.USER_ROLE + "," + UserType.MERCHANT_ROLE + "')")
    @GetMapping("/reviewRequests")
    @ResponseBody
    public String getReviewRequestTemplate() throws IOException {
        Template template = handlebarsTemplateLoader.getTemplate("extUserTransferRequests");
        return template.apply("");
    }

    @PreAuthorize("hasAnyAuthority('" + UserType.USER_ROLE + "')")
    @GetMapping("/newAccountRequests")
    @ResponseBody
    public String getAccountRequests() throws IOException {

        User currentUser = userService.getCurrentUser();
        List<NewAccountRequestDTO> newAccountRequests = accountService.getPendingAccountsForUser(currentUser);
        HashMap<String, List<NewAccountRequestDTO>> resultMap = new HashMap<>();
        resultMap.put("newAccountRequests", newAccountRequests);
        JsonNode result = mapper.valueToTree(resultMap);
        Template template = handlebarsTemplateLoader.getTemplate("extUserViewNewAccountRequests");
        return template.apply(handlebarsTemplateLoader.getContext(result));
    }

    @PreAuthorize("hasAnyAuthority('" + UserType.USER_ROLE + "," + UserType.MERCHANT_ROLE + "')")
    @PostMapping("/raiseProfileUpdateRequest")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void raiseProfileUpdateRequest(ProfileRequestDTO requestDTO, HttpServletResponse response) throws IOException {
        if (userService.getCurrentUser().getUserType().equals(UserType.USER_ROLE) || userService.getCurrentUser().getUserType().equals(UserType.MERCHANT_ROLE)) {
            requestService.createProfileUpdateRequest(requestDTO, RequestType.UPDATE_USER_PROFILE);
        }
        response.sendRedirect("home");
    }

    @PreAuthorize("hasAnyAuthority('" + UserType.USER_ROLE + "," + UserType.MERCHANT_ROLE + "')")
    @GetMapping("/modify/{id}")
    @ResponseBody
    public String getModifyAccountTemplate(@PathVariable long id) throws IOException {
        User user = userService.getCurrentUser();
        Optional<Account> optionalAccount = accountService.getAccountById(id);
        if (optionalAccount.isPresent()) {
            User account = optionalAccount.get().getUser();
            if (account.equals(user)) {
                JsonNode result = mapper.valueToTree(account);
                Template template = handlebarsTemplateLoader.getTemplate("extUserModifyAccount");
                return template.apply(handlebarsTemplateLoader.getContext(result));
            } else {
                throw new GenericRuntimeException("You can only modify your account");
            }
        }
        throw new GenericRuntimeException("You can only modify your account");
    }

    @PreAuthorize("hasAnyAuthority('" + UserType.USER_ROLE + "," + UserType.MERCHANT_ROLE + "')")
    @PostMapping("/modifyAccount")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void changeAccountType(Long id, AccountType accountType, HttpServletResponse response) throws IOException {
        Optional<Account> account = accountService.getAccountById(id);
        User requester = userService.getCurrentUser();
        account.ifPresent(acc -> {
            if (acc.getAccountType().equals(accountType)) {
                throw new GenericRuntimeException("Account already of the requested type");
            }
            switch (accountType) {
                case CURRENT:
                    requestService.createAccountTypeChangeRequest(acc, AccountType.CURRENT, requester);
                    break;
                case SAVINGS:
                    requestService.createAccountTypeChangeRequest(acc, AccountType.SAVINGS, requester);
                    break;
                case CHECKING:
                    requestService.createAccountTypeChangeRequest(acc, AccountType.CHECKING, requester);
                    break;
                default:
                    throw new GenericRuntimeException("Account Type is not correct:" + accountType);
            }
        });
        response.sendRedirect("home");
    }
}
