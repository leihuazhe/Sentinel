package com.alibaba.csp.sentinel.adapter.dubbo.extend;

import java.util.List;
import java.util.Map;


/**
 * @author leyou
 */
public interface DemoService {
    String sayHello(String name, int n);

    int sayInt(String name, int n);

    List<String> sayList(String name, int n);

    List<User> sayListBean(String name, int n);

    Map<String,List<User>> sayMapListBean(String name, int n);

}

