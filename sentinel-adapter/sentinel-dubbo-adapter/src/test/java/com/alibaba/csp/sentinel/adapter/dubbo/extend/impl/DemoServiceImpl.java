package com.alibaba.csp.sentinel.adapter.dubbo.extend.impl;

import com.alibaba.csp.sentinel.adapter.dubbo.extend.DemoService;
import com.alibaba.csp.sentinel.adapter.dubbo.extend.User;

import java.util.List;
import java.util.Map;

/**
 * @author leyou
 */
public class DemoServiceImpl implements DemoService {
    public String sayHello(String name, int n) {
        return "Hello " + name + ", " + n;
    }

    @Override
    public int sayInt(String name, int n) {
        return 0;
    }

    @Override
    public List<String> sayList(String name, int n) {
        return null;
    }

    @Override
    public List<User> sayListBean(String name, int n) {
        return null;
    }

    @Override
    public Map<String, List<User>> sayMapListBean(String name, int n) {
        return null;
    }


}
