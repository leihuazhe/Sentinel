package com.alibaba.csp.sentinel.dashboard.service;

import com.alibaba.csp.sentinel.Constants;
import com.alibaba.csp.sentinel.concurrent.NamedThreadFactory;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.FlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.discovery.AppInfo;
import com.alibaba.csp.sentinel.dashboard.discovery.MachineInfo;
import com.alibaba.csp.sentinel.dashboard.repository.rule.InMemFlowRuleStore;
import com.alibaba.csp.sentinel.dashboard.repository.rule.InMemoryRuleRepositoryAdapter;
import com.alibaba.csp.sentinel.dashboard.repository.rule.nacos.NacosConfigUtil;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRuleProvider;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRulePublisher;
import com.alibaba.csp.sentinel.dashboard.util.NginxUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.taobao.diamond.manager.ManagerListener;
import com.taobao.diamond.manager.ManagerListenerAdapter;
import com.yunji.diamond.client.api.DiamondClient;
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
import javax.annotation.Resource;
import java.io.StringReader;
import java.util.*;
import java.util.concurrent.*;

/**
 * 处理从Redis初始化数据
 */
@Service
public class NginxLuaRedisSerivce {

    private Logger logger = LoggerFactory.getLogger(NginxLuaRedisSerivce.class);


    @Resource(name = "redisTemplateNginx")
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private InMemFlowRuleStore inMemFlowRuleStore;

//    @Autowired
//    private ConfigService configService;
//
//    /**
//     * Single-thread pool. Once the thread pool is blocked, we throw up the old task.
//     */
//    private final ExecutorService pool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
//            new ArrayBlockingQueue<Runnable>(1), new NamedThreadFactory("sentinel-nacos-ds-update"),
//            new ThreadPoolExecutor.DiscardOldestPolicy());

    private DiamondClient diamondClient = null;
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

    /**
     * nginx 与 redis对应关系
     */
    private Map<String,String> redisIps = new HashMap<>();






    private DiamondClient getDiamondClient(String dataId, ManagerListener managerListener){
        DiamondClient diamondClient = new DiamondClient();
        diamondClient.setDataId(dataId);
        diamondClient.setPollingIntervalTime(10);
        diamondClient.setTimeout(2000L);
        diamondClient.setManagerListener(managerListener);
        /* 初始化diamond */
        diamondClient.init();
        return diamondClient;
    }


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

//        //环境判断
//        String configKey = "config_env";
//        String env = AppNameUtil.getEnvConfig(configKey);
//        logger.warn("config_env:{}",env);
//
//        if("idc".equals(env)){
//             /*
//            redisIps.put("m.yunjiglobal.com","172.22.14.91");
//            redisIps.put("app.yunjiglobal.com","172.22.14.61");
//            redisIps.put("vipapp.yunjiglobal.com","172.22.14.159");
//            redisIps.put("marketing.yunjiglobal.com","172.21.153.23");
//            redisIps.put("yunjioperate.yunjiglobal.com","172.21.153.48");
//            redisIps.put("reward.yunjiglobal.com","172.21.154.203");
//            redisIps.put("ys.yunjiglobal.com","172.21.150.75");
//            redisIps.put("spe.yunjix.com","172.21.154.79");//
//            redisIps.put("user.yunjiglobal.com","172.21.154.221");
//            redisIps.put("order.yunjiglobal.com","172.21.154.235");
//            redisIps.put("search.yunjiglobal.com","172.21.154.201");
//            redisIps.put("m.yj.ink","172.21.154.87");
//            redisIps.put("item.yunjiglobal.com","172.21.153.135");
//            redisIps.put("sc.yunjiglobal.com","172.21.154.162");
//            redisIps.put("chicken.yunjiglobal.com","172.21.154.165");
//            redisIps.put("insurance.yunjiglobal.com","172.21.200.184");
//             /*
//
//        }else if("dev".equals(env)){
//            redisIps.put("t.yunjiglobal.com","172.30.220.215:14159");
//            //redisIps.put("m.yunjiglobal.com","172.30.220.215:14159");
//            //redisIps.put("local.yunjiweidian.org","172.30.222.63:14159");
//        }else if ("local".equals(env)){
//            redisIps.put("m.yunjiglobal.com","172.16.0.2:7777");
//        }

        new Thread(()->{
//            Set<Map.Entry<String, String>> set =  redisIps.entrySet();
//            for(Map.Entry<String, String> e:set){
//                transform(e.getKey(),e.getValue());
//            }

//            try{
//                configService.addListener("sentinel-nginx-init", NacosConfigUtil.GROUP_ID, new Listener() {
//                    @Override
//                    public Executor getExecutor() {
//                        return pool;
//                    }
//
//                    @Override
//                    public void receiveConfigInfo(String configInfo) {
//                        updateNginxInit(configInfo);
//                    }
//                });
//
//
//                String configInfo = configService.getConfig("sentinel-nginx-init",
//                        NacosConfigUtil.GROUP_ID, 3000);
//                updateNginxInit(configInfo);
//            }catch (Exception ex){
//                logger.error("初始化nginx配置失败：",ex);
//            }


            diamondClient = getDiamondClient("sentinel-nginx-init", new ManagerListenerAdapter() {
                @Override
                public void receiveConfigInfo(String s) {
                    updateNginxInit(s);
                }
            });
            updateNginxInit(diamondClient.getConfig());

        }).start();




    }

    public void updateNginxInit(String configInfo){
        if(StringUtils.isBlank(configInfo)){
            return;
        }
        Map<String,String> redisIps = JSON.parseObject(configInfo,Map.class);
        logger.warn("更新redisIps form:{},to:{}",this.redisIps,redisIps);
        this.redisIps = redisIps;
    }



    public List<AppInfo> getNginxAppInfo(){
        List<AppInfo> appInfoList = new ArrayList<>();

        Set<Map.Entry<String, String>> set =  redisIps.entrySet();
        for(Map.Entry<String, String> e:set){
            AppInfo appInfo = new AppInfo();
            appInfo.setApp(e.getKey());

            MachineInfo machineInfo = new MachineInfo();
            machineInfo.setApp(e.getKey());
            machineInfo.setIp(e.getValue());
            machineInfo.setPort(80);
            machineInfo.setVersion(Constants.SENTINEL_VERSION);
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
     * 转换 暂时没有处理同一nginx多个domain问题
     * @param prefix
     */
    public String transform(String prefix){
        String redis = redisIps.get(prefix);
        //判断是否存在
        if(StringUtils.isBlank(redis)){
            return prefix + " 不在对应redis列表中";
        }
        String redisInitKey = SENTINEL_NGINX + prefix;
        //说明有任务在执行，或者 执行过
        String status = stringRedisTemplate.opsForValue().get(redisInitKey);
        if("runned".equals(status)){
            return prefix + "此域名下迁移配置已操作过";
        }
        if("running".equals(status)){
            //同一域名下2分钟内不允许重复操作
            String cache = stringRedisTemplate.opsForValue().get(prefix);
            if(StringUtils.isNoneBlank(cache)){
                return "5分钟内同一域名不允许重复操作";
            }
        }
        //开始操作
        stringRedisTemplate.opsForValue().set(redisInitKey,"running");
        stringRedisTemplate.opsForValue().set(prefix,redis,5, TimeUnit.MINUTES);

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
                //判断是否已经是配置的，如果是跳过
                if(newKey.startsWith("/")){
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
                    logger.warn("newKey:{} to :{} ",newKey,redisKey);
                }else{
                    logger.warn("newKey:{} not startsWith: /",newKey);
                }

            });


            publishRules(prefix);
        }catch (Exception ex){
            logger.error("操作redis",ex);
        }finally {
            redisClient.shutdown();
        }


        //做事并且写入
        stringRedisTemplate.opsForValue().set(redisInitKey,"runned");
        String val = stringRedisTemplate.opsForValue().get(redisInitKey);
        logger.warn("read:{},val:{}",prefix,val);
        return "执行迁移 doamin:" + prefix +" 下redis:"+ redis + " to  sentinel success!!";
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
