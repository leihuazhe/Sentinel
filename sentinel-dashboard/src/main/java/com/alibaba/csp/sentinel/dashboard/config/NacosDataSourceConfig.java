package com.alibaba.csp.sentinel.dashboard.config;

import com.alibaba.csp.sentinel.yj.nacos.NacosConfig;
import com.alibaba.csp.sentinel.yj.nacos.NacosDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NacosDataSourceConfig {

    @Bean
    @ConfigurationProperties("sentinel.nacos")
    public NacosConfig getNacosConfig(){
        NacosConfig nacosConfig = new NacosConfig();
        return nacosConfig;
    }

    @Bean(initMethod="start")
    public NacosDataSource getDataSource(NacosConfig nacosConfig){
        NacosDataSource nacosDataSource =  new NacosDataSource();
        nacosDataSource.setNacosConfig(nacosConfig);
        return nacosDataSource;
    }
}
