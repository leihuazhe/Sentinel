package com.alibaba.csp.sentinel.transport.util;

import java.lang.reflect.Field;

public class JagentVersionUtils {

    private  static  String VERSION = null;
    static {
        String jagentVersionClass = "com.yunji.agent.JagentVersion";
        String erlangVersionClass = "com.yunji.erlang.ErlangVersion";
        Class cls = null;
        String agent = null;
        try{
            cls = Class.forName(jagentVersionClass);
            agent = "jagent-";
        }catch (Exception ex){
            try{
                cls = Class.forName(erlangVersionClass);
                agent = "erlang-";
            }catch (Exception e){
            }
        }
        if(cls!=null){
            try{
                Field field = cls.getField("VERSION");
                VERSION = agent + (String) field.get(null);
            }catch (Exception ex){

            }

        }
    }

    public static String getVersion(){
        return VERSION;
    }
}
