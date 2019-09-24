package com.alibaba.csp.sentinel.dashboard.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.yunji.oms.common.util.JsonUtil;
import com.yunji.oms.common.util.WebUtils;
import com.yunji.sso.client.annotation.UnAuthorized;
import com.yunji.sso.client.entity.SsoResponse;
import com.yunji.sso.client.entity.SsoUser;
import com.yunji.sso.client.util.CookieUtils;
import com.yunji.sso.client.util.ThreadContextUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

public class NewLoginInterceptor extends HandlerInterceptorAdapter {

    private List<String> uncheckUrls = new ArrayList();
    private String loginUrl;
    private String tokenValidUrl;
    private String loginSuccessUrl;
    private String tokenName = "JESSIONID";
    private boolean debug = false;

    public NewLoginInterceptor() {
    }

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        Iterator var5 = this.uncheckUrls.iterator();

        String dataResponsestr;
        while(var5.hasNext()) {
            dataResponsestr = (String)var5.next();
            if (uri.startsWith(dataResponsestr)) {
                return true;
            }
        }


        if (handler.getClass().isAssignableFrom(HandlerMethod.class)) {
            UnAuthorized unAuthorized = (UnAuthorized)((HandlerMethod)handler).getMethodAnnotation(UnAuthorized.class);
            if (unAuthorized != null) {
                return true;
            }

            SsoResponse dataResponse = null;
            if (this.debug) {
                dataResponse = new SsoResponse();
                dataResponse.setCode(1);
                SsoUser ssoUser = new SsoUser();
                ssoUser.setId(111);
                ssoUser.setEmpId("0390");
                ssoUser.setUserName("徐宁");
                ssoUser.setPhone("13530334578");
                ssoUser.setLoginIdForOA("0390@yunjiweidian.com");
                ssoUser.setDepartment("研发部门");
                ssoUser.setEmail("aa@qq.com");
                dataResponse.setData(ssoUser);
                request.setAttribute("account", dataResponse.getData());
                ThreadContextUtil.put("account", dataResponse.getData());
                return true;
            }

            try {
                String jessionId = CookieUtils.getCookieValueByName(request, this.tokenName);
                if (StringUtils.isEmpty(jessionId)) {
                    this.redirectToLogin(request, response);
                    return false;
                }

                Map<String, String> header = new HashMap(1);
                header.put("Accept", "application/json");
                String result = WebUtils.doGet(this.tokenValidUrl + "/" + jessionId, Collections.EMPTY_MAP, "UTF-8", header);
                dataResponse = (SsoResponse) JsonUtil.jsonString2Bean(result, new TypeReference<SsoResponse<SsoUser>>() {
                });
                if (dataResponse == null || dataResponse.getCode() != 1) {
                    this.redirectToLogin(request, response);
                    return false;
                }

                request.setAttribute("account", dataResponse.getData());
                ThreadContextUtil.put("account", dataResponse.getData());
            } catch (IOException var10) {
                var10.printStackTrace();
                return false;
            }
        }

        return true;
    }

    private void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        boolean isAjax = request.getHeader("X-Requested-With") != null;
        if (isAjax) {
            response.setHeader("Access-Control-Allow-Origin","*");
            response.setStatus(302);
            response.setContentType("application/json");
            response.setHeader("Location",this.loginUrl + "?backUrl=" + this.loginSuccessUrl);
        } else {
            response.setHeader("Access-Control-Allow-Origin","*");
            response.setStatus(302);
            response.sendRedirect(this.loginUrl + "?backUrl=" + this.loginSuccessUrl);
        }
    }

    public String getTokenName() {
        return this.tokenName;
    }

    public void setTokenName(String tokenName) {
        this.tokenName = tokenName;
    }

    public String getTokenValidUrl() {
        return this.tokenValidUrl;
    }

    public void setTokenValidUrl(String tokenValidUrl) {
        this.tokenValidUrl = tokenValidUrl;
    }

    public List<String> getUncheckUrls() {
        return this.uncheckUrls;
    }

    public void setUncheckUrls(List<String> uncheckUrls) {
        this.uncheckUrls = uncheckUrls;
    }

    public String getLoginUrl() {
        return this.loginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public String getLoginSuccessUrl() {
        return this.loginSuccessUrl;
    }

    public void setLoginSuccessUrl(String loginSuccessUrl) {
        this.loginSuccessUrl = loginSuccessUrl;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}

