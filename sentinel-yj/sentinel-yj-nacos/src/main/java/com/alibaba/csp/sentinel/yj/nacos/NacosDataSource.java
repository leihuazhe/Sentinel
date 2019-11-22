package com.alibaba.csp.sentinel.yj.nacos;

import com.alibaba.csp.sentinel.cluster.ClusterStateManager;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientAssignConfig;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientConfig;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientConfigManager;
import com.alibaba.csp.sentinel.cluster.flow.rule.ClusterFlowRuleManager;
import com.alibaba.csp.sentinel.cluster.flow.rule.ClusterParamFlowRuleManager;
import com.alibaba.csp.sentinel.cluster.server.config.ClusterServerConfigManager;
import com.alibaba.csp.sentinel.cluster.server.config.ServerTransportConfig;
import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.property.SentinelProperty;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityRule;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.alibaba.csp.sentinel.slots.system.SystemRule;
import com.alibaba.csp.sentinel.slots.system.SystemRuleManager;
import com.alibaba.csp.sentinel.transport.config.TransportConfig;
import com.alibaba.csp.sentinel.util.AppNameUtil;
import com.alibaba.csp.sentinel.util.HostNameUtil;
import com.alibaba.csp.sentinel.util.function.Function;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;

import java.util.ArrayList;
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


    /**
     * cc for `cluster-client`
     */
    public static final String CLUSTER_MAP_DATA_ID_POSTFIX = "-cluster-map";
    public static final String CLIENT_CONFIG_POSTFIX = "-cluster-client-config";

    /**
     * cs for `cluster-server`
     */
    public static final String SERVER_TRANSPORT_CONFIG_DATA_ID_POSTFIX = "-cs-transport-config";
    public static final String SERVER_FLOW_CONFIG_DATA_ID_POSTFIX = "-cs-flow-config";
    public static final String SERVER_NAMESPACE_SET_DATA_ID_POSTFIX = "-cs-namespace-set";


    private NacosConfig nacosConfig;
    private NacosConfig nacosClusterConfig;
    private ConfigService configService = null;
    private Properties properties = new Properties();


    /**
     * 启动
     */
    public void start(){
        properties.put(PropertyKeyConst.SERVER_ADDR, nacosConfig.getRemoteAddress());
        if (nacosConfig.getNamespace()) {
            properties.put(PropertyKeyConst.NAMESPACE, nacosConfig.getNamespaceId());
        }
        try{
            this.configService = NacosFactory.createConfigService(this.properties);
        }catch (Exception e){
            RecordLog.warn("[NacosDataSource] Error occurred when initializing Nacos data source", e);
        }

        // Register client dynamic rule data source.
        initDynamicRuleProperty(AppNameUtil.getAppName(),this.nacosConfig);


        if(nacosClusterConfig!=null){
            /**client cluster*/
            // Register token client related data source.
            // Token client common config:
            initClientConfigProperty(AppNameUtil.getAppName(),this.nacosClusterConfig);
            // Token client assign config (e.g. target token server) retrieved from assign map:
            initClientServerAssignProperty(AppNameUtil.getAppName(),this.nacosClusterConfig);

            /** cluster */
            // Register token server related data source.
            // Register dynamic rule data source supplier for token server:
            registerClusterRuleSupplier(AppNameUtil.getAppName(),this.nacosConfig);
            // Token server transport config extracted from assign map:
            initServerTransportConfigProperty(AppNameUtil.getAppName(),this.nacosClusterConfig);

            // Init cluster state property for extracting mode from cluster map data source.
            initStateProperty(AppNameUtil.getAppName(),this.nacosClusterConfig);
        }

    }


    /**
     * 动态流控
     * @param appName
     * @param nacosConfig
     */
    private void initDynamicRuleProperty(String appName,final NacosConfig nacosConfig) {

        String flowDataId = appName + FLOW_DATA_ID_POSTFIX ;
        String systemDataId = appName + SYSTEM_DATA_ID_POSTFIX ;
        String authorityDataId = appName + AUTHORITY_DATA_ID_POSTFIX ;
        String paramDataId = appName + PARAM_FLOW_DATA_ID_POSTFIX ;

        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, nacosConfig.getRemoteAddress());
        properties.put(PropertyKeyConst.NAMESPACE, nacosConfig.getNamespaceId());

        //限流
        ReadableDataSource<String, List<FlowRule>> flowRuleDataSource = new CustomNacosDataSource<>(configService,properties, nacosConfig.getGroupId(), flowDataId,
                new Converter<String, List<FlowRule>>() {
                    @Override
                    public List<FlowRule> convert(String source) {
                        return JSON.parseObject(source, new TypeReference<List<FlowRule>>() {});
                    }
                });
        FlowRuleManager.register2Property(flowRuleDataSource.getProperty());

        //系统
        ReadableDataSource<String, List<SystemRule>> systemRuleDataSource = new CustomNacosDataSource<>(configService,properties, nacosConfig.getGroupId(), systemDataId,
                new Converter<String, List<SystemRule>>() {
                    @Override
                    public List<SystemRule> convert(String source) {
                        return JSON.parseObject(source, new TypeReference<List<SystemRule>>() {});
                    }
                });
        SystemRuleManager.register2Property(systemRuleDataSource.getProperty());

        //黑白名单
        ReadableDataSource<String, List<AuthorityRule>> authorityRuleDataSource = new CustomNacosDataSource<>(configService,properties, nacosConfig.getGroupId(), authorityDataId,
                new Converter<String, List<AuthorityRule>>() {
                    @Override
                    public List<AuthorityRule> convert(String source) {
                        List<RuleEx<AuthorityRule>> ruleExList = JSON.parseObject(source, new TypeReference<List<RuleEx<AuthorityRule>>>() {});

                        List<AuthorityRule> list = new ArrayList<>();
                        if(ruleExList==null){
                            return list;
                        }
                        for (RuleEx<AuthorityRule> ruleEx:ruleExList){
                            if(ruleEx!=null){
                                list.add(ruleEx.getRule());
                            }
                        }
                        return list;
                    }
                });
        AuthorityRuleManager.register2Property(authorityRuleDataSource.getProperty());

        //热点参数
        ReadableDataSource<String, List<ParamFlowRule>> paramRuleDataSource = new CustomNacosDataSource<>(configService,properties, nacosConfig.getGroupId(), paramDataId,
                new Converter<String, List<ParamFlowRule>>() {
                    @Override
                    public List<ParamFlowRule> convert(String source) {
                        List<RuleEx<ParamFlowRule>> ruleExList = JSON.parseObject(source, new TypeReference<List<RuleEx<ParamFlowRule>>>() {});
                        List<ParamFlowRule> list = new ArrayList<>();
                        if(ruleExList==null){
                            return list;
                        }
                        for (RuleEx<ParamFlowRule> ruleEx:ruleExList){
                            if(ruleEx!=null){
                                list.add(ruleEx.getRule());
                            }
                        }
                        return list;
                    }
                });
        ParamFlowRuleManager.register2Property(paramRuleDataSource.getProperty());

    }


    /**
     * 集群下Client配置
     * @param appName
     * @param nacosConfig
     */
    private void initClientConfigProperty(String appName,final NacosConfig nacosConfig) {
        String configDataId = appName + CLIENT_CONFIG_POSTFIX;
        ReadableDataSource<String, ClusterClientConfig> clientConfigDs = new CustomNacosDataSource<>(configService, properties, nacosConfig.getGroupId(),
                configDataId, new Converter<String, ClusterClientConfig>() {
            @Override
            public ClusterClientConfig convert(String source) {
                return JSON.parseObject(source, new TypeReference<ClusterClientConfig>() {});
            }
        });
        ClusterClientConfigManager.registerClientConfigProperty(clientConfigDs.getProperty());
    }


    /**
     * 集群服务配置
     * @param appName
     * @param nacosConfig
     */
    private void initServerTransportConfigProperty(String appName,NacosConfig nacosConfig) {
        String clusterMapDataId = appName + CLUSTER_MAP_DATA_ID_POSTFIX;
        ReadableDataSource<String, ServerTransportConfig> serverTransportDs = new CustomNacosDataSource<>(configService, properties, nacosConfig.getGroupId(),
                clusterMapDataId, new Converter<String, ServerTransportConfig>() {
            @Override
            public ServerTransportConfig convert(String source) {
                List<ClusterGroupEntity> groupList = JSON.parseObject(source, new TypeReference<List<ClusterGroupEntity>>() {});
                if(groupList!=null){
                    return extractServerTransportConfig(groupList);
                }
                return null;
            }
        });

        ClusterServerConfigManager.registerServerTransportProperty(serverTransportDs.getProperty());
    }



    private void registerClusterRuleSupplier(String appName,final NacosConfig nacosConfig) {
        // Register cluster flow rule property supplier which creates data source by namespace.
        // Flow rule dataId format: ${namespace}-flow-rules
        ClusterFlowRuleManager.setPropertySupplier(new Function<String, SentinelProperty<List<FlowRule>>>() {
            @Override
            public SentinelProperty<List<FlowRule>> apply(String namespace) {
                ReadableDataSource<String, List<FlowRule>> ds = new CustomNacosDataSource<>(configService, properties, nacosConfig.getGroupId(),
                        namespace + FLOW_DATA_ID_POSTFIX, new Converter<String, List<FlowRule>>() {
                    @Override
                    public List<FlowRule> convert(String source) {
                        return JSON.parseObject(source, new TypeReference<List<FlowRule>>() {});
                    }
                });
                return ds.getProperty();
            }
        });

        // Register cluster parameter flow rule property supplier which creates data source by namespace.
        ClusterParamFlowRuleManager.setPropertySupplier(new Function<String, SentinelProperty<List<ParamFlowRule>>>() {
            @Override
            public SentinelProperty<List<ParamFlowRule>> apply(String namespace) {
                ReadableDataSource<String, List<ParamFlowRule>> ds = new CustomNacosDataSource<>(configService, properties, nacosConfig.getGroupId(),
                        namespace + PARAM_FLOW_DATA_ID_POSTFIX, new Converter<String, List<ParamFlowRule>>() {
                    @Override
                    public List<ParamFlowRule> convert(String source) {
                        return JSON.parseObject(source, new TypeReference<List<ParamFlowRule>>() {});
                    }
                });
                return ds.getProperty();
            }
        });
    }


    private void initClientServerAssignProperty(String appName,final NacosConfig nacosConfig) {
        // Cluster map format:
        // [{"clientSet":["112.12.88.66@8729","112.12.88.67@8727"],"ip":"112.12.88.68","machineId":"112.12.88.68@8728","port":11111}]
        // machineId: <ip@commandPort>, commandPort for port exposed to Sentinel dashboard (transport module)
        String clusterMapDataId = appName + CLUSTER_MAP_DATA_ID_POSTFIX;
        ReadableDataSource<String, ClusterClientAssignConfig> clientAssignDs = new CustomNacosDataSource<>(configService, properties, nacosConfig.getGroupId(),
                clusterMapDataId, new Converter<String, ClusterClientAssignConfig>() {
            @Override
            public ClusterClientAssignConfig convert(String source) {
                List<ClusterGroupEntity> groupList = JSON.parseObject(source, new TypeReference<List<ClusterGroupEntity>>() {});
                return groupList==null?null:extractClientAssignment(groupList);
            }
        });
        ClusterClientConfigManager.registerServerAssignProperty(clientAssignDs.getProperty());
    }


    private void initStateProperty(String appName,final NacosConfig nacosConfig) {
        // Cluster map format:
        // [{"clientSet":["112.12.88.66@8729","112.12.88.67@8727"],"ip":"112.12.88.68","machineId":"112.12.88.68@8728","port":11111}]
        // machineId: <ip@commandPort>, commandPort for port exposed to Sentinel dashboard (transport module)
        String clusterMapDataId = appName + CLUSTER_MAP_DATA_ID_POSTFIX;
        ReadableDataSource<String, Integer> clusterModeDs =  new CustomNacosDataSource<>(configService, properties, nacosConfig.getGroupId(),
                clusterMapDataId, new Converter<String, Integer>() {
            @Override
            public Integer convert(String source) {
                List<ClusterGroupEntity> groupList = JSON.parseObject(source, new TypeReference<List<ClusterGroupEntity>>() {});
                if(groupList!=null){
                    return extractMode(groupList);
                }
                return ClusterStateManager.CLUSTER_NOT_STARTED;
            }
        });
        ClusterStateManager.registerProperty(clusterModeDs.getProperty());
    }

    private int extractMode(List<ClusterGroupEntity> groupList) {
        // If any server group machineId matches current, then it's token server.
        for(ClusterGroupEntity clusterGroupEntity:groupList){
            if(this.machineEqual(clusterGroupEntity)){
                return ClusterStateManager.CLUSTER_SERVER;
            }
        }

        // If current machine belongs to any of the token server group, then it's token client.
        // Otherwise it's unassigned, should be set to NOT_STARTED.
        for(ClusterGroupEntity clusterGroupEntity:groupList){
            if(clusterGroupEntity.getClientSet()!=null){
               for(String str:clusterGroupEntity.getClientSet()){
                   if(getCurrentMachineId().equals(str)){
                       return ClusterStateManager.CLUSTER_CLIENT;
                   }
                }
            }
        }
        return ClusterStateManager.CLUSTER_NOT_STARTED;
    }


    private ServerTransportConfig extractServerTransportConfig(List<ClusterGroupEntity> groupList) {
        for(ClusterGroupEntity groupEntity:groupList){
            if(this.machineEqual(groupEntity)){
                return new ServerTransportConfig().setPort(groupEntity.getPort()).setIdleSeconds(600);
            }
        }
        return null;
    }

    private ClusterClientAssignConfig extractClientAssignment(List<ClusterGroupEntity> groupList) {
        for(ClusterGroupEntity clusterGroupEntity:groupList){
            if(this.machineEqual(clusterGroupEntity)){
                return null;
            }
        }
        // Build client assign config from the client set of target server group.
        for (ClusterGroupEntity group : groupList) {
            if (group.getClientSet().contains(getCurrentMachineId())) {
                String ip = group.getIp();
                Integer port = group.getPort();
                return new ClusterClientAssignConfig(ip, port);
            }
        }
        return null;
    }


    private boolean machineEqual(/*@Valid*/ ClusterGroupEntity group) {
        return getCurrentMachineId().equals(group.getMachineId());
    }

    private String getCurrentMachineId() {
        // Note: this may not work well for container-based env.
        return HostNameUtil.getIp() + SEPARATOR + TransportConfig.getRuntimePort();
    }

    private static final String SEPARATOR = "@";


    public NacosConfig getNacosConfig() {
        return nacosConfig;
    }

    public void setNacosConfig(NacosConfig nacosConfig) {
        this.nacosConfig = nacosConfig;
    }

    public NacosConfig getNacosClusterConfig() {
        return nacosClusterConfig;
    }

    public void setNacosClusterConfig(NacosConfig nacosClusterConfig) {
        this.nacosClusterConfig = nacosClusterConfig;
    }
}
