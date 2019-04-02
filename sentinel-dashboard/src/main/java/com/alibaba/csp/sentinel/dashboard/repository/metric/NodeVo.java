package com.alibaba.csp.sentinel.dashboard.repository.metric;

import org.apache.commons.lang.time.DateUtils;
import org.influxdb.annotation.Column;

public class NodeVo {

    private String id;
    private String parentId;
    @Column(name="resource",tag = true)
    private String resource;

    private Integer threadNum;
    private Integer passQps;
    @Column(name = "blockQps")
    private String blockQps;
    private Long totalQps;
    @Column(name = "rt")
    private String averageRt;
    @Column(name = "successQps")
    private String successQps;
    @Column(name = "exceptionQps")
    private String exceptionQps;
    private Long oneMinutePass;
    private Long oneMinuteBlock;
    private Long oneMinuteException;
    private Long oneMinuteTotal;


    private Long timestamp;

    @Column(name = "time")
    private String time;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public Integer getThreadNum() {
        return threadNum;
    }

    public void setThreadNum(Integer threadNum) {
        this.threadNum = threadNum;
    }

    public Integer getPassQps() {
        return passQps;
    }

    public void setPassQps(Integer passQps) {
        this.passQps = passQps;
    }

    public String getBlockQps() {
        return blockQps;
    }

    public void setBlockQps(String blockQps) {
        this.blockQps = blockQps;
    }

    public Long getTotalQps() {
        return totalQps;
    }

    public void setTotalQps(Long totalQps) {
        this.totalQps = totalQps;
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

    public Long getOneMinutePass() {
        return oneMinutePass;
    }

    public void setOneMinutePass(Long oneMinutePass) {
        this.oneMinutePass = oneMinutePass;
    }

    public Long getOneMinuteBlock() {
        return oneMinuteBlock;
    }

    public void setOneMinuteBlock(Long oneMinuteBlock) {
        this.oneMinuteBlock = oneMinuteBlock;
    }

    public Long getOneMinuteException() {
        return oneMinuteException;
    }

    public void setOneMinuteException(Long oneMinuteException) {
        this.oneMinuteException = oneMinuteException;
    }

    public Long getOneMinuteTotal() {
        return oneMinuteTotal;
    }

    public void setOneMinuteTotal(Long oneMinuteTotal) {
        this.oneMinuteTotal = oneMinuteTotal;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
        try{
            this.timestamp = DateUtils.parseDate(time,new String[]{"yyyy-MM-dd'T'HH:mm:ss.SSSZ"}).getTime();
        }catch (Exception ex){

        }


    }
}
