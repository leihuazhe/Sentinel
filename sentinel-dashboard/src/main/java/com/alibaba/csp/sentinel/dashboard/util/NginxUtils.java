package com.alibaba.csp.sentinel.dashboard.util;

import com.alibaba.csp.sentinel.util.StringUtil;
import org.apache.commons.lang3.StringUtils;

public class NginxUtils {

    /**
     * 去除 "http:"前缀
     * @param url
     * @return
     */
    public static String excludeHttpPre(String url){
        if(StringUtil.isNotBlank(url)){
            if(url.startsWith("http:")){
                return url.substring(4);
            }
            if(url.startsWith("https:")){
                return url.substring(5);
            }

        }
        return url;
    }

    /**
     * 加上https前缀
     * @param url
     * @return
     */
    public static String includeHttpPre(String url){
        if(StringUtil.isNotBlank(url) && (!url.startsWith("http:") || url.startsWith("https:") )){
            return "https:" + url;
        }
        return url;
    }


    public static String getEnvConfig(String configKey){
        String env = System.getProperty(configKey);
        if(StringUtils.isBlank(env)){
            env = System.getProperty(configKey.toUpperCase());
            if(StringUtils.isBlank(env)){
                env = System.getenv(configKey);
                if(StringUtils.isBlank(env)){
                    env = System.getenv(configKey.toUpperCase());
                }
            }

            if(StringUtils.isBlank(env)){
                throw new NullPointerException("未正确读取到环境配置信息");
            }

        }
        return env;
    }
}
