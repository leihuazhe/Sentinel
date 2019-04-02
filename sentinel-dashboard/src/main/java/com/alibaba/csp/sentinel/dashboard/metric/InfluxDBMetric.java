package com.alibaba.csp.sentinel.dashboard.metric;

import java.io.Serializable;


/**
 *
 */
public class InfluxDBMetric implements Serializable {

    private Object field;

    private String key;

    private Integer tabIndex;

    private long time;

    private Object tag;


    public Object getField() {
        return field;
    }

    public void setField(Object field) {
        this.field = field;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Integer getTabIndex() {
        return tabIndex;
    }

    public void setTabIndex(Integer tabIndex) {
        this.tabIndex = tabIndex;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public Object getTag() {
        return tag;
    }

    public void setTag(Object tag) {
        this.tag = tag;
    }

    public InfluxDBMetric() {
    }

    public InfluxDBMetric(Object field, String key, Integer tabIndex, long time, Object tag) {
        this.field = field;
        this.key = key;
        this.tabIndex = tabIndex;
        this.time = time;
        this.tag = tag;
    }
}
