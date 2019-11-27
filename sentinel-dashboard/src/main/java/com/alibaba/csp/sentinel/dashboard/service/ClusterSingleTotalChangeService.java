package com.alibaba.csp.sentinel.dashboard.service;

import com.alibaba.csp.sentinel.concurrent.NamedThreadFactory;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.FlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.ParamFlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.discovery.AppInfo;
import com.alibaba.csp.sentinel.dashboard.discovery.AppManagement;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRuleProvider;
import com.alibaba.csp.sentinel.dashboard.tools.RedisLock;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSON;
import io.swagger.models.auth.In;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 处理集群机器变化，修改单机阈值
 */
@Service
public class ClusterSingleTotalChangeService {

    private Logger logger = LoggerFactory.getLogger(ClusterSingleTotalChangeService.class);

    @Autowired
    private RedisLock redisLock;

    @Resource(name = "redisTemplateReport")
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    @Qualifier("flowRuleNacosProvider")
    private DynamicRuleProvider<List<FlowRuleEntity>> flowRuleProvider;

    @Autowired
    @Qualifier("paramFlowRuleNacosProvider")
    private DynamicRuleProvider<List<ParamFlowRuleEntity>> paramFlowRuleProvider;

    @Autowired
    private AppManagement appManagement;

    /**
     * 集群机器数变更
     */
    private static final String CLUSTER_CHANGE_LOCK = "cluster:change:lock";

    //任务调度
    private ScheduledExecutorService deleteExpireAppExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("cluster-change-executor", true));


    @Value("${clusterChangeJobInterval}")
    private int clusterChangeJobInterval = 6;

    /**
     * 还属于最近执行周期
     */
    private static final String CLUSTER_CHANGE_LOCK_LAST = "cluster:change:lock:last";

    private static final String CLUSTER_CHANGE_NAME_LAST = "cluster:change:l";
    private static final String CLUSTER_CHANGE_NAME_CURRENT = "cluster:change:c";

    @PostConstruct
    public void init(){
        if(clusterChangeJobInterval<6){
            clusterChangeJobInterval = 6;
        }
        //清空
        deleteExpireAppExecutor.scheduleWithFixedDelay( ()-> matchTotalChange(), 60 * clusterChangeJobInterval,  60 * clusterChangeJobInterval, TimeUnit.SECONDS);
    }

    /**
     * 处理项目下机器变动
     */
    public void matchTotalChange(){
        //获取分布式锁
        final String requestId = UUID.randomUUID().toString();
        boolean lock = redisLock.tryLock(CLUSTER_CHANGE_LOCK, requestId ,10, TimeUnit.MINUTES);
        if(!lock){
            logger.info("get lock {} fail",CLUSTER_CHANGE_LOCK);
            return;
        }
        //如果执行周期不够，返回
        if(stringRedisTemplate.hasKey(CLUSTER_CHANGE_LOCK_LAST)){
            logger.info("get lock_last {}:{} ",CLUSTER_CHANGE_LOCK_LAST,stringRedisTemplate.opsForValue().get(CLUSTER_CHANGE_LOCK_LAST));
            return;
        }
        try{
            //删除
            stringRedisTemplate.delete(CLUSTER_CHANGE_NAME_LAST);
            stringRedisTemplate.renameIfAbsent(CLUSTER_CHANGE_NAME_CURRENT,CLUSTER_CHANGE_NAME_LAST);
            //拿出所有，要计算的项目(例：机器数) current
            Set<AppInfo> appInfos = appManagement.getBriefApps();
            Map<String,Integer> currentMachines = new HashMap<>();
            appInfos.stream().forEach((appInfo -> currentMachines.put(appInfo.getApp(),appInfo.getMachinesSize())));

            stringRedisTemplate.opsForValue().set(CLUSTER_CHANGE_NAME_CURRENT, JSON.toJSONString(currentMachines));
            //拿出上次数据 (拿出失败，或者数据为空 return ) last
            String lastString  = stringRedisTemplate.opsForValue().get(CLUSTER_CHANGE_NAME_LAST);
            if(StringUtil.isEmpty(lastString)){
                logger.info("last machines redis value is NULL");
                return;
            }
            Map<String,Integer> lastMachines = JSON.parseObject(lastString,Map.class);

            //计算变化
            for(String key:currentMachines.keySet()){
                Integer currentValue = currentMachines.get(key);
                Integer lastValue = lastMachines.get(key);
                if(lastValue==null || currentValue==null){
                    continue;
                }
                if(lastValue.intValue() == currentValue.intValue()){
                    continue;
                }
                logger.warn("app:{} currentSize:{},lastSize:{}",key,currentValue,lastMachines);

                //动态变更
                //流控
                List<FlowRuleEntity>  flowRuleEntities = flowRuleProvider.getRules(key);
                if(flowRuleEntities!=null && flowRuleEntities.size() >0){
                    //
                    for(FlowRuleEntity ruleEntity:flowRuleEntities){
                        if(ruleEntity.isClusterMode()){
                            continue;
                        }
                    }
                }
                //热点
                List<ParamFlowRuleEntity> paramFlowRuleEntities = paramFlowRuleProvider.getRules(key);
                if(paramFlowRuleEntities!=null && paramFlowRuleEntities.size() >0){
                    //
                    for(ParamFlowRuleEntity paramFlowRuleEntity:paramFlowRuleEntities){
                        if(paramFlowRuleEntity.isClusterMode()){
                            continue;
                        }
                    }
                }

            }


        }catch (Exception ex){
            logger.warn("matchTotalChange",ex);
        }finally {
            redisLock.releaseLock(CLUSTER_CHANGE_LOCK,requestId);
            stringRedisTemplate.opsForValue().set(CLUSTER_CHANGE_LOCK_LAST,String.valueOf(System.currentTimeMillis()),(60 * clusterChangeJobInterval - 10),TimeUnit.SECONDS);
        }

    }

}
