package com.alibaba.csp.sentinel.adapter.dubbo.extend;

public class User {


    public User() {
        System.out.println("aaaa");
    }

    private String name;

        public String getName() {
            return name;
        }

        public User setName(String name) {
            this.name = name;
            return this;
        }

}
