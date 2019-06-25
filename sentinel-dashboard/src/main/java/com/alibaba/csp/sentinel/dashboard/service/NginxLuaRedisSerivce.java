package com.alibaba.csp.sentinel.dashboard.service;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.FlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.discovery.AppInfo;
import com.alibaba.csp.sentinel.dashboard.discovery.MachineInfo;
import com.alibaba.csp.sentinel.dashboard.repository.rule.InMemFlowRuleStore;
import com.alibaba.csp.sentinel.dashboard.repository.rule.InMemoryRuleRepositoryAdapter;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRuleProvider;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRulePublisher;
import com.alibaba.csp.sentinel.dashboard.util.NginxUtils;
import com.alibaba.csp.sentinel.util.AppNameUtil;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

    private static final String MAX_STR = "max_";

    private static final String SENTINEL_NGINX = "sentinel:nginx:";


    @Autowired
    private InMemoryRuleRepositoryAdapter<FlowRuleEntity> repository;

    @Autowired
    @Qualifier("flowRuleNacosProvider")
    private DynamicRuleProvider<List<FlowRuleEntity>> ruleProvider;

    @Autowired
    @Qualifier("flowRuleNacosPublisher")
    private DynamicRulePublisher<List<FlowRuleEntity>> rulePublisher;

    private Map<String,String> redisIps = new HashMap<>();

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
        String configKey = "config_env";
        String env = AppNameUtil.getEnvConfig(configKey);
        logger.warn("config_env:{}",env);

        if("idc".equals(env)){
            /*
            redisIps.put("m.yunjiglobal.com","172.22.14.91");
            redisIps.put("app.yunjiglobal.com","172.22.14.61");
            redisIps.put("vipapp.yunjiglobal.com","172.22.14.159");
            redisIps.put("marketing.yunjiglobal.com","172.21.153.23");
            redisIps.put("yunjioperate.yunjiglobal.com","172.21.153.48");
            redisIps.put("reward.yunjiglobal.com","172.21.154.203");
            redisIps.put("ys.yunjiglobal.com","172.21.150.75");
            redisIps.put("spe.yunjix.com","172.21.154.79");//
            redisIps.put("user.yunjiglobal.com","172.21.154.221");
            redisIps.put("order.yunjiglobal.com","172.21.154.235");
            redisIps.put("search.yunjiglobal.com","172.21.154.201");
            redisIps.put("m.yj.ink","172.21.154.87");
            redisIps.put("item.yunjiglobal.com","172.21.153.135");
            redisIps.put("sc.yunjiglobal.com","172.21.154.162");
            redisIps.put("chicken.yunjiglobal.com","172.21.154.165");
            redisIps.put("insurance.yunjiglobal.com","172.21.200.184");

             */
        }else if("dev".equals(env)){
            redisIps.put("t.yunjiglobal.com","172.30.220.215:14159");
            //redisIps.put("m.yunjiglobal.com","172.30.220.215:14159");
            //redisIps.put("local.yunjiweidian.org","172.30.222.63:14159");
        }else if ("local".equals(env)){
            redisIps.put("m.yunjiglobal.com","172.16.0.2:7777");
        }

        new Thread(()->{
            Set<Map.Entry<String, String>> set =  redisIps.entrySet();
            for(Map.Entry<String, String> e:set){
                transform(e.getKey(),e.getValue());
            }

        }).start();



    }


    public List<AppInfo> getNginxAppInfo(){
        List<AppInfo> appInfoList = new ArrayList<>();

        Set<Map.Entry<String, String>> set =  redisIps.entrySet();
        for(Map.Entry<String, String> e:set){
            AppInfo appInfo = new AppInfo();
            appInfo.setApp(e.getKey());

            MachineInfo machineInfo = new MachineInfo();
            machineInfo.setApp(e.getKey());
            machineInfo.setIp("127.0.0.1");
            machineInfo.setPort(80);
            machineInfo.setVersion("1.5.1");
            machineInfo.setHostname("nginx-pc");
            machineInfo.setLastHeartbeat(System.currentTimeMillis());
            machineInfo.setHeartbeatVersion(System.currentTimeMillis()-10);

            appInfo.addMachine(machineInfo);
            appInfoList.add(appInfo);
        }

        return appInfoList;

    }
    /**
     * 清空原有数据
     */
    public void clear(String prefix){
        logger.warn("begin clear old redis data");
        stringRedisTemplate.keys(MAX_STR +prefix+ "*").stream().forEach((key)->{
            stringRedisTemplate.delete(key);
        });
    }


    /**
     * 转换
     * @param prefix
     * @param redis
     */
    public void transform(String prefix,String redis){
        String redisInitKey = SENTINEL_NGINX + prefix;
        String val = stringRedisTemplate.opsForValue().get(redisInitKey);
        if(SENTINEL_NGINX.equals(val)){
            logger.warn(prefix + " " + SENTINEL_NGINX + " has exits");
            return;
        }


        if(redis.split(":").length==1){
            redis +=":6379";
        }
        logger.warn("transform prefix:{},redis:{}",prefix,redis);
        RedisClient redisClient = RedisClient.create("redis://@"+redis);


        clear(prefix);
        try(StatefulRedisConnection<String, String> connection = redisClient.connect()){
            RedisCommands<String, String> syncCommands = connection.sync();
            List<String> keys = syncCommands.keys(MAX_STR + "*");
            keys.stream().forEach((key)->{
                //创建实体
                String newKey = key.substring(MAX_STR.length());
                String redisKey = prefix + NginxUtils.excludeHttpPre(newKey);
                String msg = syncCommands.hget(MSG_LIST,newKey);
                String value = syncCommands.get(MAX_STR + newKey);
                FlowRuleEntity flowRuleEntity = new FlowRuleEntity();
                flowRuleEntity.setApp(prefix);
                flowRuleEntity.setResource(redisKey);
                flowRuleEntity.setCount(NumberUtils.toDouble(value,0));
                flowRuleEntity.setAdapterText(msg);
                flowRuleEntity.setAdapterResultOn(true);
                flowRuleEntity.setAdapterType(3);
                flowRuleEntity.setGrade(1);//1为qps
                flowRuleEntity.setStrategy(0);
                flowRuleEntity.setControlBehavior(0);
                flowRuleEntity.setGmtCreate(new Date());
                flowRuleEntity.setGmtModified(flowRuleEntity.getGmtCreate());
                flowRuleEntity.setLimitApp("default");

                inMemFlowRuleStore.save(flowRuleEntity);
            });


            publishRules(prefix);
        }catch (Exception ex){
            logger.error("操作redis",ex);
        }finally {
            redisClient.shutdown();
        }


        //做事并且写入
        stringRedisTemplate.opsForValue().set(redisInitKey,SENTINEL_NGINX);
        val = stringRedisTemplate.opsForValue().get(SENTINEL_NGINX);
        logger.warn("read:{},val:{}",prefix,val);

    }

    public void save(Object entity){
        try{
            FlowRuleEntity flowRule = (FlowRuleEntity) entity;
            //先写在这
            //key限流
            flowRule.setLimitApp("default");
            flowRule.setAdapterType(3);
            flowRule.setAdapterResultOn(true);

            String url = NginxUtils.excludeHttpPre(flowRule.getResource());
            String key = MAX_STR + url;
            logger.warn("save nginx url:{},value:{},msg:{}",url,flowRule.getCount(),flowRule.getAdapterText());
            stringRedisTemplate.opsForValue().set(key,String.valueOf(flowRule.getCount().intValue()));
            //HASH返回值
            if(StringUtils.isNotBlank(flowRule.getAdapterText())){
                stringRedisTemplate.boundHashOps(MSG_LIST).put(url,flowRule.getAdapterText());
            }

        }catch (Exception ex){
            logger.error("nginx save redis",ex);
        }

    }

    public void delete(Object entity){
        FlowRuleEntity flowRule = (FlowRuleEntity) entity;
        String url = NginxUtils.excludeHttpPre(flowRule.getResource());
        String key = MAX_STR + url;

        stringRedisTemplate.delete(key);
        //HASH返回值
        stringRedisTemplate.boundHashOps(MSG_LIST).delete(url);

    }


    private void publishRules(/*@NonNull*/ String app) throws Exception {
        List<FlowRuleEntity> rules = repository.findAllByApp(app);
        rulePublisher.publish(app, rules);
    }

}
