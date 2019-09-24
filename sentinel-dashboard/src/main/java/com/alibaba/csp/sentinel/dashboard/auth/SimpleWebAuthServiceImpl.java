package com.alibaba.csp.sentinel.dashboard.auth;

import com.yunji.sso.client.entity.SsoUser;
import com.yunji.sso.client.util.ThreadContextUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * @author cdfive
 * @since 1.6.0
 */
@Component
@Primary
@ConditionalOnProperty(name = "auth.enabled", matchIfMissing = true)
public class SimpleWebAuthServiceImpl implements AuthService<HttpServletRequest> {


    @Override
    public AuthUser getAuthUser(HttpServletRequest request) {
        HttpSession session = request.getSession();

        SsoUser ssoUser  = (SsoUser) request.getAttribute(com.yunji.sso.client.util.Constants.LOGIN_ACCOUNT);

        if (ssoUser != null) {
            AuthUser authUser = new SimpleWebAuthUserImpl(ssoUser.getUserName());
            return authUser;
        }

        return null;
    }

    public static final class SimpleWebAuthUserImpl implements AuthUser {

        private String username;

        public SimpleWebAuthUserImpl(String username) {
            this.username = username;
        }

        @Override
        public boolean authTarget(String target, PrivilegeType privilegeType) {
            return true;
        }

        @Override
        public boolean isSuperUser() {
            return true;
        }

        @Override
        public String getNickName() {
            return username;
        }

        @Override
        public String getLoginName() {
            return username;
        }

        @Override
        public String getId() {
            return username;
        }
    }
}

