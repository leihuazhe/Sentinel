package com.alibaba.csp.sentinel.yj.report;

/**
 * kafka配置信息
 */
public class KafkaConfig {

    private String servers;

    private String topic;

    public String getServers() {
        return servers;
    }

    public void setServers(String servers) {
        this.servers = servers;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}
