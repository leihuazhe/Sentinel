package com.alibaba.csp.sentinel.adapter.servlet.callback;

import com.alibaba.csp.sentinel.adapter.servlet.util.FilterUtil;
import com.alibaba.csp.sentinel.slots.block.AbstractRule;
import com.alibaba.csp.sentinel.slots.block.BlockException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class CustomUrlBlockHandler implements UrlBlockHandler{
    @Override
    public void blocked(HttpServletRequest request, HttpServletResponse response, BlockException ex) throws IOException {
        //判断异常中Rule是否支持可定义返回
        AbstractRule abstractRule = ex.getRule();
        if(abstractRule.getAdapterResultOn() && abstractRule.getAdapterType() == 2){
            if(abstractRule.getAdapterResult()==null){
                synchronized (abstractRule){
                    if(abstractRule.getAdapterResult()==null){
                        try {

                            if(abstractRule.getAdapterWebType()==1){
                                response.setContentType("application/json; charset=UTF-8");
                            }else if(abstractRule.getAdapterWebType()==2){
                                response.setContentType("text/xml; charset=UTF-8");
                            }

                            writeDefaultBlockedPage(response,abstractRule.getAdapterText());
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                        return;



                    }
                }
            }

        }
        FilterUtil.blockRequest(request, response);
    }


    private static void writeDefaultBlockedPage(HttpServletResponse response,String msg) throws IOException {
        PrintWriter out = response.getWriter();
        out.print(msg);
        out.flush();
        out.close();
    }
}
