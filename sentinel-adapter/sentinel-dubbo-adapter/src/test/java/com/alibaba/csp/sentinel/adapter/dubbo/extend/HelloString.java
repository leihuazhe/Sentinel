package com.alibaba.csp.sentinel.adapter.dubbo.extend;

public class HelloString {
    public static String chageStr(String str){
        return str = "abc";
    }

    public static void main(String[] args) {
        String str = "aaa";
        str = chageStr(str);
        System.out.println(str);
    }
}
