package com.alibaba.csp.sentinel.adapter.dubbo.extend;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.google.gson.Gson;
import org.junit.Test;
import org.springframework.cglib.proxy.Enhancer;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExtendTest {

    private User u = new User();

    /**
     * 方法返回值信息
     */
    @Test
    public void testGetMethod(){
        try{
            String interfaceName = "com.alibaba.csp.sentinel.adapter.dubbo.extend.DemoService";
            String methodName = "sayList";
            Class cls = Class.forName(interfaceName);

            Method method = cls.getMethod(methodName,String.class,int.class);

//            Gson gson = new Gson();
//            gson.fromJson();

            //返回类型
            Type type = method.getReturnType();
            //带范型的返回类型
            Type type1 = method.getGenericReturnType();
            if(((Class) type).isAssignableFrom(List.class)){
                Type fc = method.getGenericReturnType();
                //如果是泛型参数类型

                if(fc instanceof ParameterizedType){
                    ParameterizedType pt = (ParameterizedType)fc;

                    System.out.println("范型");
                }
            }else if(((Class) type).isPrimitive()){
                //原始类型
                System.out.println("原始类型");
            }else{
                System.out.println("Beam类型");
            }


        }catch (Exception ex){
            ex.printStackTrace();
        }

    }


    @Test
    public void helloJsonListBean(){
        User user =new User();
        user.setName("aaa");

        List<User> list = new ArrayList<>();
        list.add(user);

        String json = JSON.toJSONString(list);
        System.out.println("JSON:"+json);

        try{
            String interfaceName = "com.alibaba.csp.sentinel.adapter.dubbo.extend.DemoService";
            String methodName = "sayListBean";
            Class cls = Class.forName(interfaceName);

            Method method = cls.getMethod(methodName,String.class,int.class);

            Gson gson = new Gson();
            Object result = gson.fromJson(json,method.getGenericReturnType());
            System.out.println(result);
        }catch (Exception ex){
            ex.printStackTrace();
        }


    }


    @Test
    public void helloJsonListString(){
        Map<String,List<User>> map = new HashMap<>();

        User user =new User();
        user.setName("aaa");

        List<User> list = new ArrayList<>();
        list.add(user);

        map.put("test",list);

        String json = JSON.toJSONString(map);
        System.out.println("JSON:"+json);

        try{
            String interfaceName = "com.alibaba.csp.sentinel.adapter.dubbo.extend.DemoService";
            String methodName = "sayMapListBean";
            Class cls = Class.forName(interfaceName);

            Method method = cls.getMethod(methodName,String.class,int.class);

            Gson gson = new Gson();
            Object result = gson.fromJson(json,method.getGenericReturnType());
            System.out.println(result);
        }catch (Exception ex){
            ex.printStackTrace();
        }


    }


    @Test
    public void helloJsonMapistString(){


        List<String> list = new ArrayList<>();
        list.add("name");

        String json = JSON.toJSONString(list);
        System.out.println("JSON:"+json);

        try{
            String interfaceName = "com.alibaba.csp.sentinel.adapter.dubbo.extend.DemoService";
            String methodName = "sayList";
            Class cls = Class.forName(interfaceName);

            Method method = cls.getMethod(methodName,String.class,int.class);

            Gson gson = new Gson();
            Object result = gson.fromJson(json,method.getGenericReturnType());
            System.out.println(result);
        }catch (Exception ex){
            ex.printStackTrace();
        }


    }


    @Test
    public void helloJsonSer(){
        String aaa = "hello";
        System.out.println(JSON.toJSONString(aaa));

        int a = 10;
        System.out.println(JSON.toJSONString(a));
    }

    @Test
    public void helloJsonDeser(){
        String aaa = "\"hello\"";
        String bbb = JSON.parseObject(aaa,String.class);
        System.out.println(bbb);

        String ccc = "10";
        Integer ddd = JSON.parseObject(ccc,Integer.class);
        System.out.println(ddd);


    }

    @Test
    public void helloStatic(){
        System.out.println(Simple.name);
    }



}
