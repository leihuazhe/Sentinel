package com.alibaba.csp.sentinel.yj.nacos;

import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.system.SystemRule;
import com.alibaba.csp.sentinel.slots.system.SystemRuleManager;
import com.alibaba.csp.sentinel.util.AppNameUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.nacos.api.PropertyKeyConst;

import java.util.List;
import java.util.Properties;

/**
 * 初始化nacos配置
 */
public class NacosDataSource {



    public static final String GROUP_ID = "SENTINEL_GROUP";

    public static final String FLOW_DATA_ID_POSTFIX = "-flow-rules";
    public static final String SYSTEM_DATA_ID_POSTFIX = "-system-rules";
    public static final String AUTHORITY_DATA_ID_POSTFIX = "-authority-rules";
    public static final String PARAM_FLOW_DATA_ID_POSTFIX = "-param-rules";
    public static final String CLUSTER_MAP_DATA_ID_POSTFIX = "-cluster-map";

    /**
     * cc for `cluster-client`
     */
    public static final String CLIENT_CONFIG_DATA_ID_POSTFIX = "-cc-config";
    /**
     * cs for `cluster-server`
     */
    public static final String SERVER_TRANSPORT_CONFIG_DATA_ID_POSTFIX = "-cs-transport-config";
    public static final String SERVER_FLOW_CONFIG_DATA_ID_POSTFIX = "-cs-flow-config";
    public static final String SERVER_NAMESPACE_SET_DATA_ID_POSTFIX = "-cs-namespace-set";


    private NacosConfig nacosConfig;


    /**
     * 启动
     */
    public void start(){
        if (nacosConfig.getNamespace()) {
            loadMyNamespaceRules();
        } else {
            loadRules();
        }

    }


    private  void loadRules() {
        String flowDataId = AppNameUtil.getAppName() + FLOW_DATA_ID_POSTFIX ;
        String systemDataId = AppNameUtil.getAppName() + SYSTEM_DATA_ID_POSTFIX ;

        //限流
        ReadableDataSource<String, List<FlowRule>> flowRuleDataSource = new com.alibaba.csp.sentinel.datasource.nacos.NacosDataSource<>(nacosConfig.getRemoteAddress(), nacosConfig.getGroupId(), flowDataId,
                new Converter<String, List<FlowRule>>() {
            @Override
            public List<FlowRule> convert(String source) {
                return JSON.parseObject(source, new TypeReference<List<FlowRule>>() {});
            }
        });

        FlowRuleManager.register2Property(flowRuleDataSource.getProperty());

        //系统
        ReadableDataSource<String, List<SystemRule>> systemRuleDataSource = new com.alibaba.csp.sentinel.datasource.nacos.NacosDataSource<>(nacosConfig.getRemoteAddress(), nacosConfig.getGroupId(), systemDataId,
                new Converter<String, List<SystemRule>>() {
                    @Override
                    public List<SystemRule> convert(String source) {
                        return JSON.parseObject(source, new TypeReference<List<SystemRule>>() {});
                    }
                });
        SystemRuleManager.register2Property(systemRuleDataSource.getProperty());
    }

    private void loadMyNamespaceRules() {

        String flowDataId = AppNameUtil.getAppName() + FLOW_DATA_ID_POSTFIX ;
        String systemDataId = AppNameUtil.getAppName() + SYSTEM_DATA_ID_POSTFIX ;

        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, nacosConfig.getRemoteAddress());
        properties.put(PropertyKeyConst.NAMESPACE, nacosConfig.getNamespaceId());

        //限流
        ReadableDataSource<String, List<FlowRule>> flowRuleDataSource = new com.alibaba.csp.sentinel.datasource.nacos.NacosDataSource<>(properties, nacosConfig.getGroupId(), flowDataId,
                new Converter<String, List<FlowRule>>() {
                    @Override
                    public List<FlowRule> convert(String source) {
                        return JSON.parseObject(source, new TypeReference<List<FlowRule>>() {});
                    }
                });
        FlowRuleManager.register2Property(flowRuleDataSource.getProperty());

        //系统
        ReadableDataSource<String, List<SystemRule>> systemRuleDataSource = new com.alibaba.csp.sentinel.datasource.nacos.NacosDataSource<>(properties, nacosConfig.getGroupId(), systemDataId,
                new Converter<String, List<SystemRule>>() {
                    @Override
                    public List<SystemRule> convert(String source) {
                        return JSON.parseObject(source, new TypeReference<List<SystemRule>>() {});
                    }
                });
        SystemRuleManager.register2Property(systemRuleDataSource.getProperty());


    }

    public NacosConfig getNacosConfig() {
        return nacosConfig;
    }

    public void setNacosConfig(NacosConfig nacosConfig) {
        this.nacosConfig = nacosConfig;
    }
}
