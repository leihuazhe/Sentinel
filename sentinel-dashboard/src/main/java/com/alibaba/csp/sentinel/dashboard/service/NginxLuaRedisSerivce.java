package com.alibaba.csp.sentinel.dashboard.service;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.FlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.repository.rule.InMemFlowRuleStore;
import com.alibaba.csp.sentinel.dashboard.repository.rule.InMemoryRuleRepositoryAdapter;
import com.alibaba.csp.sentinel.dashboard.util.NginxUtils;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.kafka.common.protocol.types.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * 处理从Redis初始化数据
 */
@Service
public class NginxLuaRedisSerivce {

    private Logger logger = LoggerFactory.getLogger(NginxLuaRedisSerivce.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private InMemFlowRuleStore inMemFlowRuleStore;

    private static final String MSG_LIST = "msg_list";

    private static final String MAX_STR = "max_/";

    private static final String SENTINEL_NGINX_LIMIT = "sentinel_nginx_limit";


    @PostConstruct
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

        //环境判断
        String env = "local";
        Map<String,String> redisIps = new HashMap<>();;
        if("idc".equals(env)){
            redisIps.put("m.yunjiglobal.com","172.22.14.91");
            redisIps.put("app.yunjiglobal.com","172.22.14.61");
            redisIps.put("vipapp.yunjiglobal.com","172.22.14.159");
            //redisIps.put(        "","172.21.153.23");
            //redisIps.put("","172.21.153.48");
            redisIps.put(        "","172.21.154.203");
            redisIps.put(       "","172.21.150.75");//
            redisIps.put(       "","172.21.154.79");
            redisIps.put(       "","172.21.154.221");
            redisIps.put(        "","172.21.154.235");
            redisIps.put(        "","172.21.154.201");
            redisIps.put(        "","172.21.154.87");
            redisIps.put(        "","172.21.153.135");
            redisIps.put(       "","172.21.154.162");
            redisIps.put(       "","172.21.154.165");
            redisIps.put(       "","172.21.200.184");
        }else if("dev".equals(env)){

        }else if ("local".equals(env)){
            redisIps.put("//m.yunjiglobal.com","172.16.0.2:7777");
        }





        String val = stringRedisTemplate.opsForValue().get(SENTINEL_NGINX_LIMIT);
        if(SENTINEL_NGINX_LIMIT.equals(val)){
            logger.warn(SENTINEL_NGINX_LIMIT + " has exits");
            //测试注释
            //return;
        }

        Set<Map.Entry<String, String>> set =  redisIps.entrySet();
        for(Map.Entry<String, String> e:set){
            transform(e.getKey(),e.getValue());
        }



        //做事并且写入
        stringRedisTemplate.opsForValue().set(SENTINEL_NGINX_LIMIT,SENTINEL_NGINX_LIMIT);
        val = stringRedisTemplate.opsForValue().get(SENTINEL_NGINX_LIMIT);
        System.out.println("val:"+val);
    }


    /**
     * 转换
     * @param prefix
     * @param redis
     */
    public void transform(String prefix,String redis){

        RedisClient redisClient = RedisClient.create("redis://@"+redis);

        try(StatefulRedisConnection<String, String> connection = redisClient.connect()){
            RedisCommands<String, String> syncCommands = connection.sync();
            List<String> keys = syncCommands.keys(MAX_STR + "*");
            keys.stream().forEach((key)->{
                //创建实体
                String redisKey = prefix + NginxUtils.excludeHttpPre(key);
                String msg = syncCommands.hget(MSG_LIST,redisKey);
                String value = syncCommands.get(MAX_STR + key);
                FlowRuleEntity flowRuleEntity = new FlowRuleEntity();
                flowRuleEntity.setApp("nginx");
                flowRuleEntity.setResource(redisKey);
                flowRuleEntity.setCount(NumberUtils.toDouble(value,0));
                flowRuleEntity.setAdapterText(msg);
                flowRuleEntity.setGrade(1);//1为qps
                flowRuleEntity.setStrategy(0);
                flowRuleEntity.setControlBehavior(0);
                flowRuleEntity.setGmtCreate(new Date());
                flowRuleEntity.setGmtModified(flowRuleEntity.getGmtCreate());


                inMemFlowRuleStore.save(flowRuleEntity);
            });
        }catch (Exception ex){
            logger.error("操作redis",ex);
        }finally {
            redisClient.shutdown();
        }




    }

    public void save(Object entity){
        try{
            FlowRuleEntity flowRule = (FlowRuleEntity) entity;
            //先写在这
            //key限流
            String url = NginxUtils.excludeHttpPre(flowRule.getResource());
            String key = MAX_STR + url;
            stringRedisTemplate.opsForValue().set(key,String.valueOf(flowRule.getCount()));
            //HASH返回值
            stringRedisTemplate.boundHashOps(MSG_LIST).put(url,flowRule.getResource());
        }catch (Exception ex){
            logger.error("nginx save redis",ex);
        }

    }












}
