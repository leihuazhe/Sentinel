package com.alibaba.csp.sentinel.dashboard.controller;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.FlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.domain.Result;
import com.alibaba.csp.sentinel.dashboard.repository.rule.InMemoryRuleRepositoryAdapter;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRuleProvider;
import com.alibaba.csp.sentinel.dashboard.service.NginxLuaRedisSerivce;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * nginx限流初始化接口
 */
@Controller
@RequestMapping(value = "/nginx", produces = MediaType.APPLICATION_JSON_VALUE)
public class NginxLimitController {

    private final Logger logger = LoggerFactory.getLogger(NginxLimitController.class);

    @Autowired
    private NginxLuaRedisSerivce nginxLuaRedisSerivce;

    @Autowired
    @Qualifier("flowRuleNacosProvider")
    private DynamicRuleProvider<List<FlowRuleEntity>> ruleProvider;

    @Autowired
    private InMemoryRuleRepositoryAdapter<FlowRuleEntity> repository;

    /**
     * 根据域名初始迁移
     * @param domain
     * @return
     * @throws BlockException
     */
    @RequestMapping("/init")
    @ResponseBody
    public String init(String domain) throws BlockException {
        return nginxLuaRedisSerivce.transform(domain);
    }

    /**
     * 根据域名初始redis
     * @param domain
     * @return
     * @throws BlockException
     */
    @RequestMapping("/initFlowData")
    @ResponseBody
    public Result<List<FlowRuleEntity>> initFlowData(String domain) throws BlockException {
        if (StringUtil.isEmpty(domain)) {
            return Result.ofFail(-1, "app can't be null or empty");
        }
        try {
            List<FlowRuleEntity> rules = ruleProvider.getRules(domain);
            if (rules != null && !rules.isEmpty()) {
                for (FlowRuleEntity entity : rules) {
                    entity.setApp(domain);
                    if (entity.getClusterConfig() != null && entity.getClusterConfig().getFlowId() != null) {
                        entity.setId(entity.getClusterConfig().getFlowId());
                    }
                }
            }
            rules = repository.saveAll(rules,true);
            return Result.ofSuccess(rules);
        } catch (Throwable throwable) {
            logger.error("Error when querying flow rules", throwable);
            return Result.ofThrowable(-1, throwable);
        }
    }


}
