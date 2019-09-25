package com.alibaba.csp.sentinel.dashboard.config;

import com.alibaba.dubbo.config.annotation.Reference;
import com.yunji.auth.api.fun.IFuncService;
import com.yunji.oms.common.service.ICacheService;
import com.yunji.oms.common.service.cache.LocalCacheServiceImpl;
import com.yunji.sso.client.interceptor.LoginInterceptor;
import com.yunji.sso.client.interceptor.PermissionInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableConfigurationProperties(SSOProperties.class)
public class SSOConfig {


    @Reference(version = "1.0.0")
    private IFuncService funcService;

    @Autowired
    private SSOProperties ssoProperties;


    /**Some urls which needn't auth, such as /auth/login,/registry/machine and so on*/
    @Value("#{'${auth.filter.exclude-urls}'.split(',')}")
    private List<String> authFilterExcludeUrls;


    @Bean
    public NewLoginInterceptor getLoginInterceptor(){
        NewLoginInterceptor loginInterceptor = new NewLoginInterceptor();
        loginInterceptor.setTokenValidUrl(ssoProperties.getTokenValidUrl());
        loginInterceptor.setDebug("true".equals(ssoProperties.getDebug()));
        loginInterceptor.setLoginSuccessUrl(ssoProperties.getLoginSuccessUrl());
        loginInterceptor.setLoginUrl(ssoProperties.getLoginUrl());
        loginInterceptor.setTokenName(ssoProperties.getTokenName());
        loginInterceptor.setUncheckUrls(authFilterExcludeUrls);

        return loginInterceptor;
    }

    @Bean
    public PermissionInterceptor getPermissionInterceptor(){
        PermissionInterceptor permissionInterceptor = new PermissionInterceptor();
        permissionInterceptor.setExpireTime(ssoProperties.getExpireTime());
        permissionInterceptor.setAppKey(ssoProperties.getAppKey());
        permissionInterceptor.setFuncService(funcService);
        permissionInterceptor.setCacheService(new LocalCacheServiceImpl());
        //设置缓存
        permissionInterceptor.setExpireTime(2 * 60);
        permissionInterceptor.setUncheckUrls(authFilterExcludeUrls);

        return permissionInterceptor;
    }

}
