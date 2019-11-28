package com.alibaba.csp.sentinel.dashboard.service;

import com.alibaba.csp.sentinel.concurrent.NamedThreadFactory;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.FlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.ParamFlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.discovery.AppInfo;
import com.alibaba.csp.sentinel.dashboard.discovery.AppManagement;
import com.alibaba.csp.sentinel.dashboard.repository.rule.InMemoryRuleRepositoryAdapter;
import com.alibaba.csp.sentinel.dashboard.repository.rule.RuleRepository;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRuleProvider;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRulePublisher;
import com.alibaba.csp.sentinel.dashboard.tools.RedisLock;
import com.alibaba.csp.sentinel.dashboard.util.MachineUtils;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSON;
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
    @Qualifier("flowRuleNacosPublisher")
    private DynamicRulePublisher<List<FlowRuleEntity>> flowRulePublisher;

    @Autowired
    @Qualifier("paramFlowRuleNacosProvider")
    private DynamicRuleProvider<List<ParamFlowRuleEntity>> paramFlowRuleProvider;

    @Autowired
    @Qualifier("paramFlowRuleNacosPublisher")
    private DynamicRulePublisher<List<ParamFlowRuleEntity>> paramFlowRulePublisher;

    @Autowired
    private AppManagement appManagement;

    @Autowired
    private InMemoryRuleRepositoryAdapter<FlowRuleEntity> flowRuleRepository;

    @Autowired
    private RuleRepository<ParamFlowRuleEntity, Long> paramFlowRuleRepository;

    /**
     * 集群机器数变更
     */
    private static final String CLUSTER_CHANGE_LOCK = "sentinel:cluster:change:lock";

    //任务调度
    private ScheduledExecutorService deleteExpireAppExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("cluster-change-executor", true));


    @Value("${clusterChangeJobInterval}")
    private int clusterChangeJobInterval = 6;

    /**
     * 还属于最近执行周期
     */
    private static final String CLUSTER_CHANGE_LOCK_LAST = "sentinel:cluster:c:lock:last";

    private static final String CLUSTER_CHANGE_NAME_CURRENT = "sentinel:cluster:c:machines";

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
            //拿出上次数据 (拿出失败，或者数据为空 return ) last
            String lastString = stringRedisTemplate.opsForValue().get(CLUSTER_CHANGE_NAME_CURRENT);

            //拿出所有，要计算的项目(例：机器数) current
            Set<AppInfo> appInfos = appManagement.getBriefApps();
            Map<String,Long> currentMachines = new HashMap<>();
            appInfos.stream().forEach(appInfo -> currentMachines.put(appInfo.getApp(),appInfo.getMachines().stream().filter(machineInfo -> machineInfo.isHealthy()).count()));

            stringRedisTemplate.opsForValue().set(CLUSTER_CHANGE_NAME_CURRENT, JSON.toJSONString(currentMachines));

            if(StringUtil.isEmpty(lastString)){
                logger.info("last machines redis value is NULL");
                return;
            }
            Map<String,Long> lastMachines = JSON.parseObject(lastString,Map.class);

            //计算变化
            for(String key:currentMachines.keySet()){
                Long currentValue = currentMachines.get(key);
                Long lastValue = lastMachines.get(key);
                if(lastValue==null || currentValue==null){
                    continue;
                }
                if(lastValue.intValue() == currentValue.intValue()){
                    continue;
                }
                logger.warn("app:{} currentSize:{},lastSize:{}",key,currentValue,lastMachines);

                //动态变更
                //流控
                AppInfo appInfo = appManagement.getDetailApp(key);
                List<FlowRuleEntity>  flowRuleEntities = flowRuleProvider.getRules(key);
                if(flowRuleEntities!=null && flowRuleEntities.size() >0){
                    //
                    try{
                        int changeCount = 0;
                        for(FlowRuleEntity ruleEntity:flowRuleEntities){
                            if(MachineUtils.calcByMachines(ruleEntity,appInfo)){
                                changeCount++;
                            }
                        }
                        if(changeCount>0){
                            flowRuleRepository.saveAll(flowRuleEntities,key,false);
                            flowRulePublisher.publish(key,flowRuleEntities);
                        }
                    }catch (Exception ex){
                        logger.error("动态变更 app:" + key + " 流控  总量平均阈值 失败",ex);
                    }

                }
                //热点
                List<ParamFlowRuleEntity> paramFlowRuleEntities = paramFlowRuleProvider.getRules(key);
                if(paramFlowRuleEntities!=null && paramFlowRuleEntities.size() >0){
                    //
                    try{
                        int changeCount = 0;
                        for(ParamFlowRuleEntity paramFlowRuleEntity:paramFlowRuleEntities){
                            if(MachineUtils.calcByMachines(paramFlowRuleEntity,appInfo)){
                                changeCount++;
                            }
                        }
                        if(changeCount>0){
                            paramFlowRuleRepository.saveAll(paramFlowRuleEntities,key,false);
                            paramFlowRulePublisher.publish(key,paramFlowRuleEntities);
                        }
                    }catch (Exception ex){
                        logger.error("动态变更 app:" + key + " 热点参数 总量平均阈值 失败",ex);
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
