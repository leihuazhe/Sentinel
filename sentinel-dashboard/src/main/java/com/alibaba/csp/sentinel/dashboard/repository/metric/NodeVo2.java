package com.alibaba.csp.sentinel.dashboard.repository.metric;

import org.influxdb.annotation.Column;

public class NodeVo2 {


    @Column(name = "blockQps")
    private String blockQps;


    @Column(name = "rt")
    private String averageRt;

    @Column(name = "successQps")
    private String successQps;

    @Column(name = "exceptionQps")
    private String exceptionQps;

    @Column(name = "time")
    private String time;

    @Column(name = "passQps")
    private String passQps;


    public String getBlockQps() {
        return blockQps;
    }

    public void setBlockQps(String blockQps) {
        this.blockQps = blockQps;
    }

    public String getAverageRt() {
        return averageRt;
    }

    public void setAverageRt(String averageRt) {
        this.averageRt = averageRt;
    }

    public String getSuccessQps() {
        return successQps;
    }

    public void setSuccessQps(String successQps) {
        this.successQps = successQps;
    }

    public String getExceptionQps() {
        return exceptionQps;
    }

    public void setExceptionQps(String exceptionQps) {
        this.exceptionQps = exceptionQps;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getPassQps() {
        return passQps;
    }

    public void setPassQps(String passQps) {
        this.passQps = passQps;
    }
}
