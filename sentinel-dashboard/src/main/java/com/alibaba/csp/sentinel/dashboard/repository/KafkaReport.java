package com.alibaba.csp.sentinel.dashboard.repository;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Properties;

@Service
public class KafkaReport {

    @Value("${monitor.kafka.topic}")
    private String topic;

    @Value("${monitor.kafka.servers}")
    private String servers;


    private KafkaProducer<String, String> producer ;//生产者


    @PostConstruct
    public void init(){

        String acks = "0";
        int retries = NumberUtils.toInt("0",0);
        int batch_size = NumberUtils.toInt("",16384);
        int linger_ms = NumberUtils.toInt("",20);
        int max_block_ms = NumberUtils.toInt("",1000);
        int buffer_memory = NumberUtils.toInt("",1024*1024*32);
        String compression_type = "lz4";
        int request_timeout_ms = NumberUtils.toInt("",30000);


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

        producer = new KafkaProducer<>(props);
    }

    public void report(String value){
        producer.send(new ProducerRecord<>(topic, value));
    }
}

