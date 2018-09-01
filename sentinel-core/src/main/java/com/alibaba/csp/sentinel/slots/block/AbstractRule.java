/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.slots.block;

import java.util.Map;

/**
 * Abstract rule entity.
 *
 * @author youji.zj
 * @author Eric Zhao
 */
public abstract class AbstractRule implements Rule {

    /**
     * Resource name.
     */
    private String resource;

    /**
     * <p>
     * Application name that will be limited by origin.
     * The default limitApp is {@code default}, which means allowing all origin apps.
     * </p>
     * <p>
     * For authority rules, multiple origin name can be separated with comma (',').
     * </p>
     */
    private String limitApp;


    /**
     * 适配器类型适配器类型(默认=0|dubbo=1|WebFilter=2|nginx=3)
     */
    private int adapterType = 0;

    /**
     * 适配器开关
     */
    private boolean adapterResultOn;

    /**
     * 缓存返回对象
     */
    private Object adapterResult;

    /**
     * 返回text
     */
    private String adapterText;

    private int adapterWebType = 0 ;

    /**
     * 单机阈值模式 (0:单机 1:总量平均)
     */
    private int singleStrategy = 0;

    /**
     * 单机阈值-总量
     */
    private double singleCount;

    /**
     * 适配器属性
     */
    private Map<String,String> adapterProperties;

    public String getResource() {
        return resource;
    }

    public AbstractRule setResource(String resource) {
        this.resource = resource;
        return this;
    }

    public String getLimitApp() {
        return limitApp;
    }

    public AbstractRule setLimitApp(String limitApp) {
        this.limitApp = limitApp;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AbstractRule)) {
            return false;
        }

        AbstractRule that = (AbstractRule)o;

        if (resource == null ? that.resource != null : resource.equals(that.resource) ) {
            return false;
        }
        if (!limitAppEquals(limitApp, that.limitApp)) {
            return false;
        }
        if(adapterType!= that.adapterType || adapterWebType != that.adapterWebType || adapterResultOn != that.adapterResultOn){
            return false;
        }
        if(adapterText==null ? that.adapterText != null :adapterText.equals(that.adapterText) ){
            return false;
        }
        if(adapterProperties==null ? that.adapterProperties != null : adapterProperties.equals(that.adapterProperties) ){
            return false;
        }

        return true;
    }

    private boolean limitAppEquals(String str1, String str2) {
        if ("".equals(str1)) {
            return RuleConstant.LIMIT_APP_DEFAULT.equals(str2);
        } else if (RuleConstant.LIMIT_APP_DEFAULT.equals(str1)) {
            return "".equals(str2) || str2 == null || str1.equals(str2);
        }
        if (str1 == null) {
            return str2 == null || RuleConstant.LIMIT_APP_DEFAULT.equals(str2);
        }
        return str1.equals(str2);
    }

    public <T extends AbstractRule> T as(Class<T> clazz) {
        return (T)this;
    }

    public int getAdapterType() {
        return adapterType;
    }

    public void setAdapterType(int adapterType) {
        this.adapterType = adapterType;
    }

    public boolean getAdapterResultOn() {
        return adapterResultOn;
    }

    public void setAdapterResultOn(boolean adapterResultOn) {
        this.adapterResultOn = adapterResultOn;
    }

    public Object getAdapterResult() {
        return adapterResult;
    }

    public void setAdapterResult(Object adapterResult) {
        this.adapterResult = adapterResult;
    }

    public String getAdapterText() {
        return adapterText;
    }

    public void setAdapterText(String adapterText) {
        this.adapterText = adapterText;
    }

    public int getAdapterWebType() {
        return adapterWebType;
    }

    public void setAdapterWebType(int adapterWebType) {
        this.adapterWebType = adapterWebType;
    }

    public Map<String, String> getAdapterProperties() {
        return adapterProperties;
    }

    public void setAdapterProperties(Map<String, String> adapterProperties) {
        this.adapterProperties = adapterProperties;
    }

    public int getSingleStrategy() {
        return singleStrategy;
    }

    public void setSingleStrategy(int singleStrategy) {
        this.singleStrategy = singleStrategy;
    }

    public double getSingleCount() {
        return singleCount;
    }

    public void setSingleCount(double singleCount) {
        this.singleCount = singleCount;
    }

    @Override
    public int hashCode() {
        int result = resource != null ? resource.hashCode() : 0;
        if (!("".equals(limitApp) || RuleConstant.LIMIT_APP_DEFAULT.equals(limitApp) || limitApp == null)) {
            result = 31 * result + limitApp.hashCode();
        }
        return result;
    }
}
