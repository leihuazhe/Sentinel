package com.alibaba.csp.sentinel.yj.report;


import com.alibaba.csp.sentinel.init.InitFunc;
import com.taobao.diamond.manager.ManagerListener;
import com.taobao.diamond.manager.ManagerListenerAdapter;
import com.yunji.diamond.client.api.DiamondClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.util.Properties;


public class ReportInit implements InitFunc {

    private DiamondClient nacosDiamondClient = null;

    private MetricKafkaReport metricKafkaReport = new MetricKafkaReport();

    private static Logger logger = LoggerFactory.getLogger(ReportInit.class);

    @Override
    public void init() throws Exception {

        nacosDiamondClient = getDiamondClient("sentinel_kafka_config", new ManagerListenerAdapter() {
            @Override
            public void receiveConfigInfo(String s) {
                managerListerner(s,true);
            }
        });
        managerListerner(nacosDiamondClient.getConfig(),false);
    }

    private void managerListerner(String config,boolean reload){
        Properties properties = new Properties();

        try (StringReader stringReader = new StringReader(config)) {
            properties.load(stringReader);
            metricKafkaReport.start(properties,reload);
        }catch (Exception ex){
            logger.error("init sentinel kafka fail",ex);
        }
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

}
