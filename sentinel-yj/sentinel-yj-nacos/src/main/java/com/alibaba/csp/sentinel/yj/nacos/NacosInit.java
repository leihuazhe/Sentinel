package com.alibaba.csp.sentinel.yj.nacos;

import com.alibaba.csp.sentinel.config.SentinelConfig;
import com.alibaba.csp.sentinel.init.InitFunc;
import com.alibaba.csp.sentinel.init.InitOrder;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSON;
import com.taobao.diamond.manager.ManagerListener;
import com.yunji.diamond.client.api.DiamondClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

/**
 *  nacos配置信息拉取并初始化
 */
@InitOrder(InitOrder.HIGHEST_PRECEDENCE)
public class NacosInit implements InitFunc {

    private DiamondClient nacosDiamondClient = null;

    @Override
    public void init() throws Exception {
        initNacos();

    }


    private DiamondClient getDiamondClient(String dataId, ManagerListener managerListener){
        DiamondClient diamondClient = new DiamondClient();
        diamondClient.setDataId(dataId);
        diamondClient.setPollingIntervalTime(10);
        diamondClient.setTimeout(2000L);
        if(diamondClient!=null){
            diamondClient.setManagerListener(managerListener);
        }
        /* 初始化diamond */
        diamondClient.init();
        return diamondClient;
    }

    private void initNacos(){

        /**
         * 暂时不支持动态更新
         */
        nacosDiamondClient = getDiamondClient("buriedpoint_nacos_config",null);

        Properties properties = new Properties();

        try (StringReader stringReader = new StringReader(nacosDiamondClient.getConfig())){
            properties.load(stringReader);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(StringUtil.isNotBlank(properties.getProperty("csp.sentinel.dashboard.server"))){
            SentinelConfig.setConfigIfAbsent("csp.sentinel.dashboard.server",properties.getProperty("csp.sentinel.dashboard.server"));
        }

        int heartbeat = NumberUtils.toInt(properties.getProperty("csp.sentinel.heartbeat.interval.ms"),-1);
        if(heartbeat>0){
            SentinelConfig.setConfig("csp.sentinel.heartbeat.interval.ms",properties.getProperty("csp.sentinel.heartbeat.interval.ms"));
        }


        NacosConfig nacosConfig = new NacosConfig();
        nacosConfig.setGroupId(properties.getProperty("groupId"));
        nacosConfig.setNamespaceId(properties.getProperty("namespaceId"));
        nacosConfig.setRemoteAddress(properties.getProperty("remoteAddress"));

        //nacos集群配置
        String clusterConfig = properties.getProperty("sentinel.cluster.config");
        NacosConfig nacosConfigCluster = null;
        if(StringUtils.isNoneBlank(clusterConfig)){
            try{
                nacosConfigCluster = JSON.parseObject(clusterConfig,NacosConfig.class);
            }catch (Exception ex){

            }

        }




        NacosDataSource nacosDataSource = new NacosDataSource();
        nacosDataSource.setNacosConfig(nacosConfig);
        nacosDataSource.setNacosClusterConfig(nacosConfigCluster);
        nacosDataSource.start();


    }



}
