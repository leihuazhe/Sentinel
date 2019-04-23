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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.alibaba.csp.sentinel.util.AssertUtil;

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import static java.util.stream.Collectors.toList;

/**
 * @author leyou
 */
@Component
public class SimpleMachineDiscovery implements MachineDiscovery {


    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final String SENTINEL_APPS = "sentinel:apps:";

    private final String SENTINEL_APPS_KEYS= "k:"+SENTINEL_APPS;



    @Override
    public long addMachine(MachineInfo machineInfo) {
        AssertUtil.notNull(machineInfo, "machineInfo cannot be null");
        String appKey = SENTINEL_APPS + machineInfo.getApp();
        String key = machineInfo.getIp() + ":" + machineInfo.getPort();
        stringRedisTemplate.boundHashOps(appKey).put(key, JSON.toJSONString(machineInfo));

        if(!stringRedisTemplate.boundSetOps(SENTINEL_APPS_KEYS).isMember(machineInfo.getApp())){
            stringRedisTemplate.boundSetOps(SENTINEL_APPS_KEYS).add(machineInfo.getApp());
        }
        return 1;
    }

    @Override
    public boolean removeMachine(String app, String ip, int port) {
        AssertUtil.assertNotBlank(app, "app name cannot be blank");
        String appKey = SENTINEL_APPS + app;
        String key = ip + ":" + port;
        stringRedisTemplate.boundHashOps(appKey).delete(key);
        return true;
    }

    @Override
    public List<String> getAppNames() {
//        Set<String> set = stringRedisTemplate.keys(SENTINEL_APPS + "*");
        Set<String> set = stringRedisTemplate.boundSetOps(SENTINEL_APPS_KEYS).members();
        return set.stream().map(str-> str.replaceAll(SENTINEL_APPS,"")).collect(toList());
    }

    @Override
    public AppInfo getDetailApp(String app) {
        AssertUtil.assertNotBlank(app, "app name cannot be blank");
        String appKey = SENTINEL_APPS + app;
        AppInfo appInfo = new AppInfo();
        appInfo.setApp(app);
        Map<Object,Object> maps = stringRedisTemplate.opsForHash().entries(appKey);
        if(maps!=null){
            Set<Map.Entry<Object, Object>> entrySet = maps.entrySet();
            for (Map.Entry<Object, Object> entry : entrySet) {
                MachineInfo machineInfo = JSON.parseObject(entry.getValue().toString(),MachineInfo.class);
                if(machineInfo!=null){
                    appInfo.addMachine(machineInfo);
                }
            }
        }
        return appInfo;
    }

    @Override
    public Set<AppInfo> getBriefApps() {
        Set<AppInfo> appInfoSet = new HashSet<>();
        List<String> appNames = getAppNames();
        for (String str:appNames) {
            appInfoSet.add(getDetailApp(str));
        }
        return appInfoSet;
    }

    @Override
    public void removeApp(String app) {
        AssertUtil.assertNotBlank(app, "app name cannot be blank");
        //apps.remove(app);
        stringRedisTemplate.delete(SENTINEL_APPS + app);
        stringRedisTemplate.boundSetOps(SENTINEL_APPS_KEYS).remove(app);
    }

}
