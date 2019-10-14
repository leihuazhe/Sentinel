/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.dashboard.config;

import com.alibaba.csp.sentinel.adapter.servlet.CommonFilter;
import com.alibaba.csp.sentinel.dashboard.auth.AuthService;
import com.alibaba.csp.sentinel.dashboard.auth.AuthService.AuthUser;
import com.yunji.sso.client.interceptor.LoginInterceptor;
import com.yunji.sso.client.interceptor.PermissionInterceptor;
import org.apache.commons.lang.StringUtils;
import com.alibaba.csp.sentinel.dashboard.filter.AuthFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.config.annotation.*;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.Filter;

/**
 * @author leyou
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final Logger logger = LoggerFactory.getLogger(WebConfig.class);

    @Autowired
    private AuthFilter authFilter;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**").addResourceLocations("classpath:/resources/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.htm");
    }

/**
     * Add {@link CommonFilter} to the server, this is the simplest way to use Sentinel
     * for Web application.
     */
    @Bean
    public FilterRegistrationBean sentinelFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CommonFilter());
        registration.addUrlPatterns("/*");
        registration.setName("sentinelFilter");
        registration.setOrder(1);

        logger.info("Sentinel servlet CommonFilter registered");

        return registration;
    }


    /**Some urls which needn't auth, such as /auth/login,/registry/machine and so on*/
    @Value("#{'${auth.filter.exclude-urls}'.split(',')}")
    private List<String> authFilterExcludeUrls;

    /**Some urls with suffixes which needn't auth, such as htm,html,js and so on*/
    @Value("#{'${auth.filter.exclude-url-suffixes}'.split(',')}")
    private List<String> authFilterExcludeUrlSuffixes;

    @Value("${sso.loginUrl}")
    private String loginUrl;

    @Value("${sso.loginSuccessUrl}")
    private String loginSuccessUrl;


    private static final String URL_SUFFIX_DOT = ".";

//    @Bean
//    public FilterRegistrationBean authenticationFilterRegistration() {
//        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
//        registration.setFilter(new Filter() {
//
//            @Override
//            public void init(FilterConfig filterConfig) throws ServletException { }
//
//            @Override
//            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
//                                 FilterChain chain) throws IOException, ServletException {
//                HttpServletRequest request = (HttpServletRequest)servletRequest;
//                HttpServletResponse response = (HttpServletResponse) servletResponse;
//
//                String servletPath = request.getServletPath();
//
//                // Exclude the urls which needn't auth
//                if (authFilterExcludeUrls.contains(servletPath)) {
//                    chain.doFilter(request, response);
//                    return;
//                }
//
//                // Exclude the urls with suffixes which needn't auth
//                for (String authFilterExcludeUrlSuffix : authFilterExcludeUrlSuffixes) {
//                    if (StringUtils.isBlank(authFilterExcludeUrlSuffix)) {
//                        continue;
//                    }
//
//                    // Add . for url suffix so that we needn't add . in property file
//                    if (!authFilterExcludeUrlSuffix.startsWith(URL_SUFFIX_DOT)) {
//                        authFilterExcludeUrlSuffix = URL_SUFFIX_DOT + authFilterExcludeUrlSuffix;
//                    }
//
//                    if (servletPath.endsWith(authFilterExcludeUrlSuffix)) {
//                        chain.doFilter(request, response);
//                        return;
//                    }
//                }
//
//                //目录只对 "/" 进行判断
////                if("/".equals(servletPath) && !"true".equals(ssoDebug)){
//                if(!"true".equals(ssoDebug)){
//                    AuthUser authUser = authService.getAuthUser(request);
//                    // authentication fail
//                    if (authUser == null) {
//                        response.setStatus(302);
//                        response.setContentType("application/json");
//                        response.sendRedirect(loginUrl + "?backUrl=" + loginSuccessUrl);
//                        return;
//                    }
//                }
//
//                chain.doFilter(servletRequest, servletResponse);
//            }
//
//            @Override
//            public void destroy() { }
//        });
//        registration.addUrlPatterns("/*");
//        registration.setName("authenticationFilter");
//        registration.setOrder(0);
//        return registration;
//    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "TRACE");
    }


    @Autowired
    private NewLoginInterceptor loginInterceptor;

    @Autowired
    private PermissionInterceptor permissionInterceptor;

    @Value("${sso.debug}")
    private String ssoDebug;

    @Value("${auth.enabled}")
    private String authEnabled;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if("true".equals(authEnabled)){
            registry.addInterceptor(loginInterceptor);
            registry.addInterceptor(permissionInterceptor);
        }
    }
}
