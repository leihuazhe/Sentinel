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
package com.alibaba.csp.sentinel.dashboard.discovery;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import com.alibaba.csp.sentinel.dashboard.service.NginxLuaRedisSerivce;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class AppManagement implements MachineDiscovery {

    @Autowired
    private ApplicationContext context;

    private MachineDiscovery machineDiscovery;

    @Autowired
    private NginxLuaRedisSerivce nginxLuaRedisSerivce;

    @PostConstruct
    public void init() {
        machineDiscovery = context.getBean(SimpleMachineDiscovery.class);
    }

    @Override
    public Set<AppInfo> getBriefApps() {
        Set<AppInfo> appInfos = machineDiscovery.getBriefApps();
        appInfos.addAll(nginxLuaRedisSerivce.getNginxAppInfo());
        return appInfos;
    }

    @Override
    public long addMachine(MachineInfo machineInfo) {
        return machineDiscovery.addMachine(machineInfo);
    }
    
    @Override
    public boolean removeMachine(String app, String ip, int port) {
        return machineDiscovery.removeMachine(app, ip, port);
    }

    @Override
    public List<String> getAppNames() {
        return machineDiscovery.getAppNames();
    }

    @Override
    public AppInfo getDetailApp(String app) {
        List<AppInfo> nginxAppInfo = nginxLuaRedisSerivce.getNginxAppInfo();
        if(nginxAppInfo!=null && nginxAppInfo.size()>0){
            for(AppInfo appInfo:nginxAppInfo){
                if(appInfo.getApp().equals(app)){
                    return appInfo;
                }
            }
        }
        return machineDiscovery.getDetailApp(app);
    }
    
    @Override
    public void removeApp(String app) {
        machineDiscovery.removeApp(app);
    }

}
