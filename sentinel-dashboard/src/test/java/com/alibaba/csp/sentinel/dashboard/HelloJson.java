package com.alibaba.csp.sentinel.dashboard;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HelloJson {
    public static void main(String[] args) {
        try{
            JSONArray jsonArray = JSONArray.parseArray(FileUtils.readFileToString(new File("D:\\workspace\\team\\github\\wangchongya\\Sentinel-new\\sentinel-dashboard\\src\\test\\java\\com\\alibaba\\csp\\sentinel\\dashboard\\app.json")));
            System.out.println("total size:" + jsonArray.size());

            List<String> outLine = new ArrayList<>(jsonArray.size() * 2);
            for (int i = 0,len= jsonArray.size(); i <len ; i++) {
                JSONObject jsonObject = (JSONObject) jsonArray.get(i);
                String app = (String) jsonObject.get("app");
                System.out.println(app);
                outLine.add(app +","+app +","+app +",sentinel控制台,Browser,菜单,全部,"+(i)+",1248");
            }

            FileUtils.writeLines(new File("d:\\abc.cvs"),outLine);

        }catch (Exception ex){
            ex.printStackTrace();
        }
    }
}
