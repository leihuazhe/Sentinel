package com.alibaba.csp.sentinel.yj.nacos;

import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.nacos.api.PropertyKeyConst;

import java.util.List;
import java.util.Properties;

/**
 * 初始化nacos配置
 */
public class NacosDataSource {

    private NacosConfig nacosConfig;


    /**
     * 启动
     */
    public void start(){

        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, nacosConfig.getRemoteAddress());
        properties.put(PropertyKeyConst.NAMESPACE, nacosConfig.getNamespaceId());

        ReadableDataSource<String, List<FlowRule>> flowRuleDataSource = new com.alibaba.csp.sentinel.datasource.nacos.NacosDataSource<>(properties, nacosConfig.getGroupId(), nacosConfig.getDataId(),
                source -> JSON.parseObject(source, new TypeReference<List<FlowRule>>() {
                }));
        FlowRuleManager.register2Property(flowRuleDataSource.getProperty());
    }

    public NacosConfig getNacosConfig() {
        return nacosConfig;
    }

    public void setNacosConfig(NacosConfig nacosConfig) {
        this.nacosConfig = nacosConfig;
    }
}
