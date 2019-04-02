package com.alibaba.csp.sentinel.adapter.dubbo.fallback;

import com.alibaba.csp.sentinel.slots.block.AbstractRule;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.SentinelRpcException;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcResult;

import com.google.gson.Gson;

import java.lang.reflect.Method;
import java.lang.reflect.Type;


/**
 * 自定义返回
 * @author yunshu
 */
public class CustomDubboFallback implements DubboFallback{
    @Override
    public Result handle(Invoker<?> invoker, Invocation invocation, BlockException ex) {
        return returnAdapterResult(invoker,invocation,ex);
    }

    public static Result returnAdapterResult(Invoker<?> invoker, Invocation invocation, BlockException ex){

        //判断异常中Rule是否支持可定义返回
        AbstractRule abstractRule = ex.getRule();
        if(abstractRule.getAdapterResultOn() && abstractRule.getAdapterType() == 1){
            if(abstractRule.getAdapterResult()==null){
                synchronized (abstractRule){
                    if(abstractRule.getAdapterResult()==null){
                        try {
                            //TDTD
                            Method method = invoker.getInterface().getMethod(invocation.getMethodName(),invocation.getParameterTypes());
                            Type type = method.getGenericReturnType();


                            Gson gson = new Gson();
                            Object result = gson.fromJson(abstractRule.getAdapterText(),type);
                            abstractRule.setAdapterResult(result);

/*
//fastjson
                            if(((Class) type).isAssignableFrom(List.class)){
                                Type fc = method.getGenericReturnType();
                                //如果是泛型参数类型
                                if(fc instanceof ParameterizedType){
                                    abstractRule.setAdapterResult(JSONArray.parseArray(abstractRule.getAdapterText(),((ParameterizedType) fc).getActualTypeArguments()[0].getClass()));
                                }
                            }else if(((Class) type).isPrimitive()){
                                //原始类型
                                abstractRule.setAdapterResult(JSON.parseObject(abstractRule.getAdapterText(),type.getClass()));
                            }else{
                                abstractRule.setAdapterResult(JSON.parseObject(abstractRule.getAdapterText(),type.getClass()));
                            }

                            abstractRule.setAdapterResult(null);
*/
                        }catch (Exception e){
                            e.printStackTrace();
                        }



                    }
                }
            }
            return new RpcResult(abstractRule.getAdapterResult());
        }

        // Just wrap and throw the exception.
        throw new SentinelRpcException(ex);


    }
}
