/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.dashboard.controller;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import com.alibaba.csp.sentinel.dashboard.discovery.AppInfo;
import com.alibaba.csp.sentinel.dashboard.discovery.AppManagement;
import com.alibaba.csp.sentinel.dashboard.discovery.MachineInfo;
import com.alibaba.csp.sentinel.dashboard.domain.Result;
import com.alibaba.csp.sentinel.dashboard.domain.vo.MachineInfoVo;
import com.yunji.auth.entity.func.FuncVo;
import com.yunji.sso.client.entity.SsoUser;
import com.yunji.sso.client.util.ThreadContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Carpenter Lee
 */
@RestController
@RequestMapping(value = "/app")
public class AppController {

    @Autowired
    private AppManagement appManagement;

    @Value("${sso.allappinfo.debug}")
    private String ssoShowAllDebug;

    @Value("#{'${sso.allappinfo.ids}'.split(',')}")
    private List<String> ssoAllappInfoIds;

    @GetMapping("/names.json")
    public Result<List<String>> queryApps(HttpServletRequest request) {
        return Result.ofSuccess(appManagement.getAppNames());
    }

    @GetMapping("/briefinfos.json")
    public Result<List<AppInfo>> queryAppInfos(HttpServletRequest request) {
        boolean debug = "true".equals(ssoShowAllDebug);
        List<AppInfo> list = null;

        SsoUser ssoUser=((SsoUser)ThreadContextUtil.get(com.yunji.sso.client.util.Constants.LOGIN_ACCOUNT));
        if(!debug && ssoUser!=null){
            if(ssoAllappInfoIds.contains(ssoUser.getId().toString())){
                debug = true;
            }
        }
        Set<AppInfo> appInfoSet =  appManagement.getBriefApps();
        List<FuncVo> funcVos = null;
        if(!debug){
            //TOTO 此处需要获取用户权限
            funcVos = (List<FuncVo>) ThreadContextUtil.get(com.yunji.sso.client.util.Constants.FUNCTION_KEY);
        }

        if(funcVos!=null && funcVos.isEmpty()){
            Set<String> funcVoMap = funcVos.stream().map(vo->vo.getFunctionUrl()).collect(Collectors.toSet());
            list = appInfoSet.stream().filter(appInfo -> funcVoMap.contains(appInfo.getApp())).collect(Collectors.toList());
        }else{
            list = new ArrayList<>(appInfoSet);
        }

        Collections.sort(list, Comparator.comparing(AppInfo::getApp));
        return Result.ofSuccess(list);
    }

    @GetMapping(value = "/{app}/machines.json")
    public Result<List<MachineInfoVo>> getMachinesByApp(@PathVariable("app") String app) {
        AppInfo appInfo = appManagement.getDetailApp(app);
        if (appInfo == null) {
            return Result.ofSuccess(null);
        }
        List<MachineInfo> list = new ArrayList<>(appInfo.getMachines());
        Collections.sort(list, (o1, o2) -> {
            int t = o1.getApp().compareTo(o2.getApp());
            if (t != 0) {
                return t;
            }
            t = o1.getIp().compareTo(o2.getIp());
            if (t != 0) {
                return t;
            }
            return o1.getPort().compareTo(o2.getPort());
        });
        return Result.ofSuccess(MachineInfoVo.fromMachineInfoList(list));
    }
    
    @RequestMapping(value = "/{app}/machine/remove.json")
    public Result<String> removeMachineById(
            @PathVariable("app") String app,
            @RequestParam(name = "ip") String ip,
            @RequestParam(name = "port") int port) {
        AppInfo appInfo = appManagement.getDetailApp(app);
        if (appInfo == null) {
            return Result.ofSuccess(null);
        }
        if (appManagement.removeMachine(app, ip, port)) {
            return Result.ofSuccessMsg("success");
        } else {
            return Result.ofFail(1, "remove failed");
        }
    }
}
