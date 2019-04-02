package com.alibaba.csp.sentinel.dashboard.service;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 处理从Redis初始化数据
 */
@Service
public class NginxLuaRedisSerivce {

    public void init(){
        //判断redis中是否有 标识位

        //拉取配置信息
        /**
         * yunjibuyer    					172.22.14.91
         * yunjiapp.   					    172.22.14.61
         * yunjiapp4buyer.   				172.22.14.159
         * yunjimarketingapp.   			172.21.153.23
         * yunjioperateapp. 				172.21.153.48
         * yunjirewardapp.   				172.21.154.203
         * yunjiysapp.   					172.21.150.75
         * yunjispecialbuyer.  			    172.21.154.79
         * yunjiuserapp. 					172.21.154.221
         * yunjiorderapp.    				172.21.154.235
         * yunji-searchweb.  				172.21.154.201
         * yunjishorturl.  				    172.21.154.87
         * yunjiitemapp.   				    172.21.153.135
         * yunjiscapp. 					    172.21.154.162
         * yunjichickengame. 				172.21.154.165
         * yunji-insuranceweb. 			    172.21.200.184
         */
        List<String> redisIps = Arrays.asList(
                "172.22.14.91",
                "172.22.14.61",
                "172.22.14.159",
                "172.21.153.23",
                "172.21.153.48",
                "172.21.154.203",
                "172.21.150.75",
                "172.21.154.79",
                "172.21.154.221",
                "172.21.154.235",
                "172.21.154.201",
                "172.21.154.87",
                "172.21.153.135",
                "172.21.154.162",
                "172.21.154.165",
                "172.21.200.184"
        );

    }







}
