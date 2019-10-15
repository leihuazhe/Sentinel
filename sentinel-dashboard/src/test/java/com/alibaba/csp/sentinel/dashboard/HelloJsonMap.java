package com.alibaba.csp.sentinel.dashboard;

import com.alibaba.fastjson.JSON;

import java.util.HashMap;
import java.util.Map;

public class HelloJsonMap {


    /**
     * nginx 与 redis对应关系
     */
    private static Map<String,String> redisIps = new HashMap<>();

    public static void main(String[] args) {
        redisIps.put("t.yunjiglobal.com","172.30.220.215:14159");
        redisIps.put("m.yunjiglobal.com","172.22.14.91");

        String strMap = JSON.toJSONString(redisIps);
        System.out.println("strMap:"+strMap);
    }
}
