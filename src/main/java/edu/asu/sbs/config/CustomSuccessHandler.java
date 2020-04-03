package edu.asu.sbs.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;


@Component
@Configuration
public class CustomSuccessHandler implements AuthenticationSuccessHandler {

    private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest arg0, HttpServletResponse arg1, Authentication authentication)
            throws IOException, ServletException {

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        authorities.forEach(authority -> {
            switch (authority.getAuthority()) {
                case UserType.MERCHANT_ROLE:
                case UserType.USER_ROLE:
                    try {
                        redirectStrategy.sendRedirect(arg0, arg1, "/api/v1/customer/home");
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    break;
                case UserType.ADMIN_ROLE:
                    try {
                        redirectStrategy.sendRedirect(arg0, arg1, "/api/v1/admin/requests");
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    break;
                case UserType.EMPLOYEE_ROLE1:
                    try {
                        redirectStrategy.sendRedirect(arg0, arg1, "/api/v1/tier1/accounts");
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    break;
                case UserType.EMPLOYEE_ROLE2:
                    try {
                        redirectStrategy.sendRedirect(arg0, arg1, "/api/v1/tier2/transactions");
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    break;
                default:
                    throw new IllegalStateException();
            }
        });
    }
}