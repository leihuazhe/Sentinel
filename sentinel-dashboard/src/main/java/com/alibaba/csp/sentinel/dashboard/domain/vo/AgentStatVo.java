package com.alibaba.csp.sentinel.dashboard.domain.vo;

import java.util.Map;

public class AgentStatVo {

    private String appName;

    private Map<String,Integer> versionMap;

    private int machineSize;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public Map<String, Integer> getVersionMap() {
        return versionMap;
    }

    public void setVersionMap(Map<String, Integer> versionMap) {
        this.versionMap = versionMap;
    }

    public int getMachineSize() {
        return machineSize;
    }

    public void setMachineSize(int machineSize) {
        this.machineSize = machineSize;
    }
}
