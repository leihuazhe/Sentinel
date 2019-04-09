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
            if(url.startsWith("http://")){
                return url.substring(6);
            }
            if(url.startsWith("https://")){
                return url.substring(7);
            }

        }
        return url;
    }

    /**
     * 加上https://、http://前缀
     * @param url
     * @return
     */
    public static String includeHttpPre(String url){
        if(StringUtil.isNotBlank(url) && (!url.startsWith("http://") || url.startsWith("https://") )){
            return "https://" + url;
        }
        return url;
    }


}
