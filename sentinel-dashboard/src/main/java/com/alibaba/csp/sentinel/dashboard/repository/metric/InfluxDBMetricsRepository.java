package com.alibaba.csp.sentinel.dashboard.repository.metric;

import com.alibaba.csp.sentinel.command.vo.NodeVo;
import com.alibaba.csp.sentinel.config.SentinelConfig;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.MetricEntity;
import com.alibaba.csp.sentinel.dashboard.discovery.MachineInfo;
import com.alibaba.csp.sentinel.dashboard.metric.InfluxDBMetric;
import com.alibaba.csp.sentinel.dashboard.repository.KafkaReport;
import com.alibaba.csp.sentinel.node.Node;
import com.alibaba.csp.sentinel.node.metric.MetricNode;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.influxdb.dto.QueryResult;
import org.influxdb.impl.InfluxDBResultMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;


/**
 * 保存到InfluxDB 现青龙提供Http接口 (拉取信息后，缓存本地并等待上传)
 */
@Component
public class InfluxDBMetricsRepository  {

    private Logger logger = LoggerFactory.getLogger(InfluxDBMetricsRepository.class);
    private static final Charset DEFAULT_CHARSET = Charset.forName(SentinelConfig.charset());


    private CloseableHttpAsyncClient httpclient;

//    public InfluxDBMetricsRepository() {
//        init();
//    }

    @Value("${monitor.report.http}")
    private String monitorReportHttp;

    @Value("${monitor.influxdb.http}")
    private String monitorInfluxdbHttp;

    @Autowired
    private KafkaReport kafkaReport;


    @PostConstruct
    public void init(){
        try{

            IOReactorConfig ioConfig = IOReactorConfig.custom()
                    .setConnectTimeout(3000)
                    .setSoTimeout(3000)
                    .setIoThreadCount(Runtime.getRuntime().availableProcessors() * 2)
                    .build();

            httpclient = HttpAsyncClients.custom()
                    .setRedirectStrategy(new DefaultRedirectStrategy() {
                        @Override
                        protected boolean isRedirectable(final String method) {
                            return false;
                        }
                    }).setMaxConnTotal(4000)
                    .setMaxConnPerRoute(1000)
                    .setDefaultIOReactorConfig(ioConfig)
                    .build();
            httpclient.start();
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }


    //@Override
    public void save(MetricEntity metric) {
        report(Arrays.asList(metric));
    }


    private void report(List<MetricEntity> metrics) {
        try{

            List<InfluxDBMetric> list = new ArrayList<>();
            for(MetricEntity metric:metrics){
                Map<String,String> tags = new HashMap<>();
                tags.put("app",metric.getApp());
                tags.put("resource",metric.getResource());
                tags.put("ip",metric.getIp());

                Map<String,Object> mapField = new HashMap<>();
                mapField.put("blockQps",metric.getBlockQps());
                mapField.put("count",metric.getCount());
                mapField.put("exceptionQps",metric.getExceptionQps());
                mapField.put("passQps",metric.getPassQps());
                mapField.put("successQps",metric.getSuccessQps());
                mapField.put("rt",metric.getRt());
                //mapField.put("resourceCode",metric.getResourceCode());
                mapField.put("gmtCreate",metric.getGmtCreate());
                mapField.put("gmtModified",metric.getGmtModified());
                mapField.put("id",metric.getId());


                list.add(new InfluxDBMetric(mapField,metric.getApp(),20,metric.getTimestamp().getTime(),tags));
            }

            final String data = JSON.toJSONString(list);


            if(kafkaReport!=null){
                kafkaReport.report(data);
                //方便调试
                //return;
            }
            final String url = monitorReportHttp + "/writeV2";
            final HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE);

            List<NameValuePair> pairs = new ArrayList<>();

            NameValuePair pair1 = new BasicNameValuePair("data", data);
            pairs.add(pair1);

            httpPost.setEntity(new UrlEncodedFormEntity(pairs, HTTP.UTF_8));

            logger.info("url:{} ,data:{}",url,data);

            httpclient.execute(httpPost, new FutureCallback<HttpResponse>() {
                @Override
                public void completed(final HttpResponse response) {

                    try{
                        int code = response.getStatusLine().getStatusCode();
                        if (code != 200) {
                            return;
                        }
                        Charset charset = null;
                        try {
                            String contentTypeStr = response.getFirstHeader("Content-type").getValue();
                            if (StringUtil.isNotEmpty(contentTypeStr)) {
                                ContentType contentType = ContentType.parse(contentTypeStr);
                                charset = contentType.getCharset();
                            }
                        } catch (Exception ignore) {
                        }
                        String body = EntityUtils.toString(response.getEntity(), charset != null ? charset : DEFAULT_CHARSET);
                        logger.info("body:{}",body);
                    }catch (Exception ex){
                        ex.printStackTrace();
                    }

                }

                @Override
                public void failed(final Exception ex) {

                }

                @Override
                public void cancelled() {
                    httpPost.abort();
                }
            });
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }


    private void handleResponse(final HttpResponse response, MachineInfo machine,
                                Map<String, MetricEntity> metricMap) throws Exception {
        int code = response.getStatusLine().getStatusCode();
        if (code != 200) {
            return;
        }
        Charset charset = null;
        try {
            String contentTypeStr = response.getFirstHeader("Content-type").getValue();
            if (StringUtil.isNotEmpty(contentTypeStr)) {
                ContentType contentType = ContentType.parse(contentTypeStr);
                charset = contentType.getCharset();
            }
        } catch (Exception ignore) {
        }
        String body = EntityUtils.toString(response.getEntity(), charset != null ? charset : DEFAULT_CHARSET);
        if (StringUtil.isEmpty(body) ) {
            //logger.info(machine.getApp() + ":" + machine.getIp() + ":" + machine.getPort() + ", bodyStr is empty");
            return;
        }
        String[] lines = body.split("\n");
        //logger.info(machine.getApp() + ":" + machine.getIp() + ":" + machine.getPort() +
        //    ", bodyStr.length()=" + body.length() + ", lines=" + lines.length);
        handleBody(lines, machine, metricMap);
    }

    private void handleBody(String[] lines, MachineInfo machine, Map<String, MetricEntity> map) {
        //logger.info("handleBody() lines=" + lines.length + ", machine=" + machine);
        if (lines.length < 1) {
            return;
        }

        for (String line : lines) {
            try {
                MetricNode node = MetricNode.fromThinString(line);
                /**
                 * aggregation metrics by app_resource_timeSecond, ignore ip and port.
                 */
                String key = buildMetricKey(machine.getApp(), node.getResource(), node.getTimestamp());
                MetricEntity entity = map.get(key);
                if (entity != null) {
                    entity.addPassQps(node.getPassQps());
                    entity.addBlockQps(node.getBlockQps());
                    entity.addRtAndSuccessQps(node.getRt(), node.getSuccessQps());
                    entity.addExceptionQps(node.getExceptionQps());
                    entity.addCount(1);
                } else {
                    entity = new MetricEntity();
                    entity.setApp(machine.getApp());
                    entity.setTimestamp(new Date(node.getTimestamp()));
                    entity.setPassQps(node.getPassQps());
                    entity.setBlockQps(node.getBlockQps());
                    entity.setRtAndSuccessQps(node.getRt(), node.getSuccessQps());
                    entity.setExceptionQps(node.getExceptionQps());
                    entity.setCount(1);
                    entity.setResource(node.getResource());
                    map.put(key, entity);
                }
            } catch (Exception e) {
                logger.warn("handleBody line exception, machine: {}, line: {}", machine.toLogString(), line);
            }
        }
    }

    private String buildMetricKey(String app, String resource, long timestamp) {
        return app + "__" + resource + "__" + (timestamp / 1000);
    }


    //@Override
    public void saveAll(Iterable<MetricEntity> metrics) {
        if (metrics == null) {
            return;
        }

        List<MetricEntity> list = new ArrayList<>();

        metrics.forEach((metricEntity)->{
            list.add(metricEntity);
        });
        report(list);
    }

    private String httpGetContent(String url) {
        final HttpGet httpGet = new HttpGet(url);
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> reference = new AtomicReference<>();
        httpclient.execute(httpGet, new FutureCallback<HttpResponse>() {
            @Override
            public void completed(final HttpResponse response) {
                try {
                    reference.set(getBody(response));
                } catch (Exception e) {
                    logger.info("httpGetContent " + url + " error:", e);
                } finally {
                    latch.countDown();
                }
            }

            @Override
            public void failed(final Exception ex) {
                latch.countDown();
                logger.info("httpGetContent " + url + " failed:", ex);
            }

            @Override
            public void cancelled() {
                latch.countDown();
            }
        });
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.info("wait http client error:", e);
        }
        return reference.get();
    }


    private String getBody(HttpResponse response) throws Exception {
        Charset charset = null;
        try {
            String contentTypeStr = response.getFirstHeader("Content-type").getValue();
            if (StringUtil.isNotEmpty(contentTypeStr)) {
                ContentType contentType = ContentType.parse(contentTypeStr);
                charset = contentType.getCharset();
            }
        } catch (Exception ignore) {
        }
        return EntityUtils.toString(response.getEntity(), charset != null ? charset : DEFAULT_CHARSET);
    }


    // @Override
    public List<MetricEntity> queryByAppAndResourceBetween(String app, String resource, long startTime, long endTime) {

        final String url = monitorInfluxdbHttp +"/query?u=admin&p=admin&db=monitor&q=";
        try {
            String q = URLEncoder.encode("select sum(blockQps) as blockQps,sum(count) as count,sum(exceptionQps)as exceptionQps,sum(passQps) as passQps,sum(rt) as rt,sum(successQps)as successQps  from \"1d\".sentinel_monitor where app='"+app+"' and resource='"+resource+"' and time >= now() - 1m group by time(5s) #query_select#"+app,"UTF-8");
            String result = httpGetContent(url + q);
            logger.debug("listResourcesOfApp:{}",result);

            QueryResult queryResult = JSON.parseObject(result,QueryResult.class);
            System.out.println("listResourcesOfApp:"+queryResult);

            List<NodeVo2> vos = new InfluxDBResultMapper().toPOJO(queryResult, NodeVo2.class, "sentinel_monitor");

            List<MetricEntity> list = new ArrayList<>();
            try{
                for(NodeVo2 vo : vos){
                    MetricEntity metricEntity = new MetricEntity();
                    metricEntity.setResource(resource);
                    metricEntity.setBlockQps(NumberUtils.toLong(vo.getBlockQps(),0));
                    metricEntity.setExceptionQps(NumberUtils.toLong(vo.getExceptionQps(),0));
                    metricEntity.setRt(NumberUtils.toLong(vo.getAverageRt(),0));
                    metricEntity.setSuccessQps(NumberUtils.toLong(vo.getSuccessQps(),0));
                    metricEntity.setPassQps(NumberUtils.toLong(vo.getPassQps(),0));
                    Date date =  DateUtils.parseDate(vo.getTime(),new String[]{"yyyy-MM-dd'T'HH:mm:ss'Z'"});
                    metricEntity.setTimestamp(date);
                    metricEntity.setGmtCreate(date);
                    metricEntity.setGmtModified(date);
                    list.add(metricEntity);
                }
            }catch (Exception ex){
                logger.warn("解析失败",ex);
            }

            return list;

        }catch (Exception ex){
            logger.warn("listResourcesOfApp",ex);
        }
        return null;
    }

    //@Override
    public List<String> listResourcesOfApp(String app) {
        List<NodeVo> list = fetchResourceOfMachine(app);
        List<String> lst = new ArrayList<>();
        for(NodeVo node:list){
            lst.add(node.getResource());
        }
        return lst;
    }


    public List<NodeVo> fetchResourceOfMachine(String app){
        List<NodeVo> list = new ArrayList<>();
        final String url = monitorInfluxdbHttp +"/query?u=admin&p=admin&db=monitor&q=";
        try {
            String q = URLEncoder.encode("select sum(successQps)/5 as successQps,sum(blockQps)/5 as blockQps,sum(exceptionQps)/5 as exceptionQps,sum(rt)/5 as rt,sum(passQps) as passQps  from \"1d\".sentinel_monitor where app='"+app+"' and time >= now() - 5m group by resource#query_select#"+app,"UTF-8");
            String result = httpGetContent(url + q);
            logger.debug("listResourcesOfApp:{}",result);

            QueryResult queryResult = JSON.parseObject(result,QueryResult.class);
            System.out.println("listResourcesOfApp:"+queryResult);

            List<com.alibaba.csp.sentinel.dashboard.repository.metric.NodeVo> vos = new InfluxDBResultMapper().toPOJO(queryResult, com.alibaba.csp.sentinel.dashboard.repository.metric.NodeVo.class, "sentinel_monitor");


            for(com.alibaba.csp.sentinel.dashboard.repository.metric.NodeVo vo : vos){
                NodeVo nodeVo = new NodeVo();
                nodeVo.setResource(vo.getResource());
                nodeVo.setBlockQps(NumberUtils.toLong(vo.getBlockQps(),0));
                nodeVo.setExceptionQps(NumberUtils.toLong(vo.getExceptionQps(),0));
                nodeVo.setAverageRt(NumberUtils.toLong(vo.getAverageRt(),0));


                nodeVo.setOneMinutePass(NumberUtils.toLong(vo.getSuccessQps(),0));
                nodeVo.setOneMinuteBlock(NumberUtils.toLong(vo.getBlockQps(),0));
                nodeVo.setOneMinuteException(NumberUtils.toLong(vo.getExceptionQps(),0));
                list.add(nodeVo);
            }

        }catch (Exception ex){
            logger.warn("listResourcesOfApp",ex);
        }
        return list;
    }



}
