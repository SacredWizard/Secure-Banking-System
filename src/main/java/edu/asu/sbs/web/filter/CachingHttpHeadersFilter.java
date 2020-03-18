package edu.asu.sbs.web.filter;

import edu.asu.sbs.config.SbsProperties;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class CachingHttpHeadersFilter implements Filter {

    public static final int DEFAULT_DAYS_TO_LIVE = 1461;
    public static final long DEFAULT_SECONDS_TO_LIVE = TimeUnit.DAYS.toMillis(DEFAULT_DAYS_TO_LIVE);
    private long cacheTimeToLive = DEFAULT_SECONDS_TO_LIVE;

    private SbsProperties sbsProperties;

    public CachingHttpHeadersFilter(SbsProperties sbsProperties) {
        this.sbsProperties = sbsProperties;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.cacheTimeToLive = TimeUnit.DAYS.toMillis(sbsProperties.getHttp().getCache().getTimeToLiveInDays());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        httpServletResponse.setHeader("Cache-Control", "max-age" + cacheTimeToLive+", public");
        httpServletResponse.setHeader("Pragma", "cache");
        httpServletResponse.setDateHeader("Expires", cacheTimeToLive + System.currentTimeMillis());

        chain.doFilter(request, response);

    }

    @Override
    public void destroy() {
        /*
        * Nothing tp destroy
        */
    }

}
