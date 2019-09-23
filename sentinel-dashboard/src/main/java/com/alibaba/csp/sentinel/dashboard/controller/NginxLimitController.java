package com.alibaba.csp.sentinel.dashboard.controller;

import com.alibaba.csp.sentinel.dashboard.service.NginxLuaRedisSerivce;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * nginx限流初始化接口
 */
@Controller
@RequestMapping(value = "/nginx", produces = MediaType.APPLICATION_JSON_VALUE)
public class NginxLimitController {


    @Autowired
    private NginxLuaRedisSerivce nginxLuaRedisSerivce;

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

}
