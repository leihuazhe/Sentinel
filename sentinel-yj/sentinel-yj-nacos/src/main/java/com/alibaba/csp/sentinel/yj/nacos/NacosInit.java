package com.alibaba.csp.sentinel.yj.nacos;

import com.alibaba.csp.sentinel.config.SentinelConfig;
import com.alibaba.csp.sentinel.init.InitFunc;
import com.alibaba.csp.sentinel.init.InitOrder;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.taobao.diamond.manager.ManagerListener;
import com.yunji.diamond.client.api.DiamondClient;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

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
        diamondClient.setManagerListener(managerListener);
        /* 初始化diamond */
        diamondClient.init();
        return diamondClient;
    }

    private void initNacos(){

        nacosDiamondClient = getDiamondClient("buriedpoint_nacos_config",null);

        Properties properties = new Properties();

        try (StringReader stringReader = new StringReader(nacosDiamondClient.getConfig())){
            properties.load(stringReader);
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



            NacosDataSource nacosDataSource = new NacosDataSource();
            nacosDataSource.setNacosConfig(nacosConfig);
            nacosDataSource.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    private void initSentinelConfig(){
//
//        sentinelConfigDiamondClient = getDiamondClient("buriedpoint_sentinel_config",null);
//
//
//        Properties properties = new Properties();
//        try {
//            properties.load(new StringReader(sentinelConfigDiamondClient.getConfig()));
//            properties.setProperty()
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}
