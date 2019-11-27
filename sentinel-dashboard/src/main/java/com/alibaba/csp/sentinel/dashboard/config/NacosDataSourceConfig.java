package com.alibaba.csp.sentinel.dashboard.config;

import com.alibaba.csp.sentinel.yj.nacos.NacosConfig;
import com.alibaba.csp.sentinel.yj.nacos.NacosDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NacosDataSourceConfig {



    @Bean(name = "defaultNacos")
    @ConfigurationProperties("sentinel.nacos")
    public com.alibaba.csp.sentinel.yj.nacos.NacosConfig getNacosConfig(){
        com.alibaba.csp.sentinel.yj.nacos.NacosConfig nacosConfig = new com.alibaba.csp.sentinel.yj.nacos.NacosConfig();
        return nacosConfig;
    }

    @Bean(name = "clusterNacos")
    @ConfigurationProperties("sentinel.cluster-nacos")
    public com.alibaba.csp.sentinel.yj.nacos.NacosConfig getNacosClusterConfig(){
        com.alibaba.csp.sentinel.yj.nacos.NacosConfig nacosConfig = new com.alibaba.csp.sentinel.yj.nacos.NacosConfig();
        return nacosConfig;
    }

    @Bean(initMethod="start")
    public NacosDataSource getDataSource(@Qualifier(value="defaultNacos") com.alibaba.csp.sentinel.yj.nacos.NacosConfig nacosConfig,
                                         @Qualifier(value="clusterNacos") com.alibaba.csp.sentinel.yj.nacos.NacosConfig nacosClusterConfig){
        NacosDataSource nacosDataSource =  new NacosDataSource();
        nacosDataSource.setNacosConfig(nacosConfig);
        nacosDataSource.setNacosClusterConfig(nacosClusterConfig);
        return nacosDataSource;
    }


}
