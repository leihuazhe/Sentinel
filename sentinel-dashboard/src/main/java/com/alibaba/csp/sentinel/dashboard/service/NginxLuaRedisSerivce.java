package com.alibaba.csp.sentinel.dashboard.service;

import com.alibaba.csp.sentinel.Constants;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.FlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.discovery.AppInfo;
import com.alibaba.csp.sentinel.dashboard.discovery.MachineInfo;
import com.alibaba.csp.sentinel.dashboard.repository.rule.InMemFlowRuleStore;
import com.alibaba.csp.sentinel.dashboard.repository.rule.InMemoryRuleRepositoryAdapter;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRulePublisher;
import com.alibaba.csp.sentinel.dashboard.util.NginxUtils;
import com.alibaba.fastjson.JSON;
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
import java.util.*;
import java.util.concurrent.TimeUnit;

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


    private static final String MSG_LIST = "msg_list";

    private static final String MAX_STR = "max_";

    private static final String SENTINEL_NGINX = "sentinel:nginx:";


    @Autowired
    private InMemoryRuleRepositoryAdapter<FlowRuleEntity> repository;

    @Autowired
    @Qualifier("flowRuleNacosPublisher")
    private DynamicRulePublisher<List<FlowRuleEntity>> rulePublisher;

    /**
     * nginx 与 redis对应关系
     */
    private Map<String,String> redisIps = new HashMap<>();

    /**
     * 对应机器数
     */
    private Map<String,Integer> nginxMachineSize = new HashMap<>();




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

        new Thread(()->{
            DiamondClient diamondClient = getDiamondClient("sentinel-nginx-init", new ManagerListenerAdapter() {
                @Override
                public void receiveConfigInfo(String s) {
                    updateNginxInit(s);
                }
            });
            updateNginxInit(diamondClient.getConfig());

            DiamondClient diamondClientNginx = getDiamondClient("sentinel-nginx-machine", new ManagerListenerAdapter() {
                @Override
                public void receiveConfigInfo(String s) {
                    updateNginxSize(s);
                }
            });
            updateNginxSize(diamondClientNginx.getConfig());

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

    public void updateNginxSize(String configInfo){
        if(StringUtils.isBlank(configInfo)){
            return;
        }
        Map<String,Integer> nginxMachineSize = JSON.parseObject(configInfo,Map.class);
        logger.warn("更新 nginxMachineSize form:{},to:{}",this.nginxMachineSize,nginxMachineSize);
        this.nginxMachineSize = nginxMachineSize;
    }


    public List<AppInfo> getNginxAppInfo(){
        List<AppInfo> appInfoList = new ArrayList<>();

        Set<Map.Entry<String, String>> set =  redisIps.entrySet();
        for(Map.Entry<String, String> e:set){
            AppInfo appInfo = new AppInfo();
            appInfo.setApp(e.getKey());
            appInfo.setAppType(1000);

            MachineInfo machineInfo = new MachineInfo();
            machineInfo.setApp(e.getKey());
            machineInfo.setIp(e.getValue());
            machineInfo.setPort(80);
            machineInfo.setVersion(Constants.SENTINEL_VERSION);
            machineInfo.setHostname("nginx-pc");
            machineInfo.setLastHeartbeat(System.currentTimeMillis());
            machineInfo.setHeartbeatVersion(System.currentTimeMillis()-10);

            appInfo.addMachine(machineInfo);
            appInfo.setMachinesSize(nginxMachineSize.get(e.getKey())==null?1:nginxMachineSize.get(e.getKey()));
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
            publicMsg(flowRule.getApp());
        }catch (Exception ex){
            logger.error("nginx save redis",ex);
        }

    }

    public boolean publicMsg(String app){
        //通知
        stringRedisTemplate.convertAndSend(app,"update");
        return true;
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
