package com.alibaba.csp.sentinel.yj.report;


import com.alibaba.csp.sentinel.concurrent.NamedThreadFactory;
import com.alibaba.csp.sentinel.config.SentinelConfig;
import com.alibaba.csp.sentinel.node.metric.MetricNode;
import com.alibaba.csp.sentinel.node.metric.MetricSearcher;
import com.alibaba.csp.sentinel.node.metric.MetricWriter;
import com.alibaba.csp.sentinel.util.AppNameUtil;
import com.alibaba.csp.sentinel.util.HostNameUtil;
import com.alibaba.csp.sentinel.util.PidUtil;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于Kafka监控日志上报
 */
public class MetricKafkaReport {


    private static Logger logger = LoggerFactory.getLogger(MetricKafkaReport.class);
    private MetricSearcher searcher;
    private final Object lock = new Object();


    private KafkaProducer<String, String> producer ;//生产者
    /**
     * 初始化状态
     */
    private AtomicBoolean START = new AtomicBoolean(false);
    private KafkaConfig kafkaConfig;

    private ScheduledExecutorService fetchScheduleService = Executors.newScheduledThreadPool(1,
            new NamedThreadFactory("sentinel-dashboard-metrics-fetch-task"));;

    private ScheduledFuture scheduledFuture = null;

    /**
     * 上报时间间隔
     */
    private long DEFAULT_INTERVALSECOND = 5;

    private long lastFetchMetricTime = 0L;


    private void start(Properties props) {
        long intervalSecond = NumberUtils.toLong(props.getProperty(""),DEFAULT_INTERVALSECOND);
        scheduledFuture = fetchScheduleService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    fetchMetric();
                } catch (Exception e) {
                    logger.info("fetchAllApp error:", e);
                }
            }
        }, intervalSecond, intervalSecond, TimeUnit.SECONDS);

        lastFetchMetricTime = System.currentTimeMillis();
    }

    private void fetchMetric(){
        long startTime = lastFetchMetricTime;
        long endTime   = lastFetchMetricTime = System.currentTimeMillis();

        List<MetricNode> list =  findByTimeAndResource(startTime,endTime,null);
        report(list);
    }

    public void start(final Properties properties, final boolean restart){
        stop();
        //kafka
        String servers = properties.getProperty("kafka.servers");
        String topic = properties.getProperty("tracemq.topic");
        if(StringUtil.isBlank(servers)){
            logger.error("kafka server is NULL");
            return;
        }
        String acks = properties.getProperty("kafka.acks");
        int retries = NumberUtils.toInt(properties.getProperty("kafka.retries"),0);
        int batch_size = NumberUtils.toInt(properties.getProperty("kafka.batch.size"),16384);
        int linger_ms = NumberUtils.toInt(properties.getProperty("kafka.linger.ms"),20);
        int max_block_ms = NumberUtils.toInt(properties.getProperty("kafka.max.block.ms"),1000);
        int buffer_memory = NumberUtils.toInt(properties.getProperty("kafka.buffer.memory"),1024*1024*32);
        String compression_type = properties.getProperty("kafka.compression.type");
        int request_timeout_ms = NumberUtils.toInt(properties.getProperty("kafka.request.timeout.ms"),30000);


        final Properties props = new Properties();
        props.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer");// 序列化的方式， ByteArraySerializer或者StringSerializer
        props.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer");
        props.put("bootstrap.servers",	servers);//该地址是集群的子集，用来探测集群。
        props.put("acks", StringUtils.isBlank(acks)?"0":acks.trim());//发送记录不在乎丢
        props.put("retries", retries);// 请求失败重试的次数
        props.put("buffer.memory", buffer_memory );// 提供给生产者缓冲内存总量32MB
        props.put("compression.type",StringUtils.isBlank(compression_type)?"none":compression_type);//压缩方式
        props.put("request.timeout.ms",request_timeout_ms);
        props.put("max.block.ms",max_block_ms);//当满后等待时间
        props.put("batch.size", batch_size);// batch的大小
        props.put("linger.ms", linger_ms);// 默认情况即使缓冲区有剩余的空间，也会立即发送请求，设置一段时间用来等待从而将缓冲区填的更多，单位为毫秒，producer发送数据会延迟1ms，可以减少发送到kafka服务器的请求数据
        props.put("client.id", "sentinel-" + topic);//解决类隔离情况下，client.id自动生成下冲突问题

        final KafkaConfig kafkaConfigTemp = new KafkaConfig();
        kafkaConfigTemp.setServers(servers.trim());
        kafkaConfigTemp.setTopic(topic);

        new Thread(new Runnable() {
            @Override
            public void run() {
                START.compareAndSet(true,false);
                logger.warn("kafka producer对象正在初始化..."+ props);
                kafkaConfig = kafkaConfigTemp;
                //保持类隔离
                //兼容,kafka默认
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                try{
                    Thread.currentThread().setContextClassLoader(null);
                    init(props);
                }catch (Exception ex2){
                    logger.warn("初始化kafka失败：",ex2);
                }finally {
                    //还原
                    Thread.currentThread().setContextClassLoader(classLoader);
                }


            }
        }
        ).start();

    }

    private void init(Properties props ){
        producer = new KafkaProducer<>(props);
        logger.warn("kafka producer对象正在初始化完成");
        start(props);
        START.compareAndSet(false,true);
    }


    public void stop(){
        if( producer!=null ){
            logger.warn("kafka producer对象close...");
            producer.close();
            producer = null;
            START.compareAndSet(true,false);
        }
        if( scheduledFuture!=null ){
            scheduledFuture.cancel(false);
        }
    }

    /**
     * 上报监控信息到kafka
     * @param metrics
     */
    private void report(List<MetricNode> metrics){
        if(producer==null || metrics!=null || metrics.isEmpty()) {
            return;
        }

        String app = AppNameUtil.getAppName();
        String localIp = HostNameUtil.getIp();
        final Map<String, MetricEntity> map = new ConcurrentHashMap<>(16);

        for(MetricNode node:metrics){
            /**
             * aggregation metrics by app_resource_timeSecond, ignore ip and port.
             */
            String key = buildMetricKey(app, node.getResource(), node.getTimestamp());
            MetricEntity entity = map.get(key);
            if (entity != null) {
                entity.addPassQps(node.getPassQps());
                entity.addBlockQps(node.getBlockQps());
                entity.addRtAndSuccessQps(node.getRt(), node.getSuccessQps());
                entity.addExceptionQps(node.getExceptionQps());
                entity.addCount(1);
                entity.setIp(localIp);
            } else {
                entity = new MetricEntity();
                entity.setApp(app);
                entity.setTimestamp(new Date(node.getTimestamp()));
                entity.setPassQps(node.getPassQps());
                entity.setBlockQps(node.getBlockQps());
                entity.setRtAndSuccessQps(node.getRt(), node.getSuccessQps());
                entity.setExceptionQps(node.getExceptionQps());
                entity.setCount(1);
                entity.setResource(node.getResource());
                entity.setIp(localIp);
                map.put(key, entity);
            }
        }

//        List<InfluxDBMetric> list = new ArrayList<>();
        for(MetricEntity metric:map.values()){
            /*
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


            InfluxDBMetric bean = new InfluxDBMetric(mapField,metric.getApp(),20,metric.getTimestamp().getTime(),tags);
            producer.send(new ProducerRecord<String, String>(kafkaConfig.getTopic(),JSON.toJSONString(bean)));
            */

            producer.send(new ProducerRecord<String, String>(kafkaConfig.getTopic(),JSON.toJSONString(metric)));
            //list.add(bean);
        }



        //final String data = JSON.toJSONString(list);
        //producer.send(new ProducerRecord<String, String>(kafkaConfig.getTopic(),data));

    }

    private String buildMetricKey(String app, String resource, long timestamp) {
        return app + "__" + resource + "__" + (timestamp / 1000);
    }


    /**
     * search
     * @param startTime
     * @param endTime
     * @param identity
     * @return
     */
    private List<MetricNode> findByTimeAndResource(long startTime,long endTime,String identity){
        if (searcher == null) {
            synchronized (lock) {
                String appName = SentinelConfig.getAppName();
                if (appName == null) {
                    appName = "";
                }
                if (searcher == null) {
                    searcher = new MetricSearcher(MetricWriter.METRIC_BASE_DIR,
                            MetricWriter.formMetricFileName(appName, PidUtil.getPid()));
                }
            }
        }
        List<MetricNode> list = null;
        try{
            list = searcher.findByTimeAndResource(startTime, endTime, identity);
        }catch (Exception ex){
            logger.warn("拉取sentinel监控日志失败",ex);
        }
        return  list;
    }
}
