package com.alibaba.csp.sentinel.dashboard.config;

import com.alibaba.fastjson.support.spring.GenericFastJsonRedisSerializer;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class LettuceRedisConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.redis.lettuce.pool")
    @Scope(value = "prototype")
    public GenericObjectPoolConfig redisPool(){
        return new GenericObjectPoolConfig();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.redis.nginx")
    public RedisProperties redisConfigNignx(){
        return new RedisProperties();
    }


    @Bean
    @ConfigurationProperties(prefix = "spring.redis.report")
    public RedisProperties redisConfigReport(){
        return new RedisProperties();
    }


    @Bean
    public LettuceConnectionFactory factoryNginx(){
        GenericObjectPoolConfig config = redisPool();

        LettuceClientConfiguration clientConfiguration = LettucePoolingClientConfiguration.builder()
                .poolConfig(config)
                //.commandTimeout(Duration.ofMillis(config.getMaxWaitMillis()))
                .build();
        return createLettuceConnectionFactory( clientConfiguration,redisConfigNignx());
    }


    @Bean
    @Primary
    public LettuceConnectionFactory factoryReport(){
        GenericObjectPoolConfig config = redisPool();
        LettuceClientConfiguration clientConfiguration = LettucePoolingClientConfiguration.builder()
                .poolConfig(config)
                //.commandTimeout(Duration.ofMillis(config.getMaxWaitMillis()))
                .build();
        return createLettuceConnectionFactory(clientConfiguration,redisConfigReport());
    }



    @Bean(name = "redisTemplateNginx")
    public StringRedisTemplate redisTemplateNginx(){
        StringRedisTemplate template = getRedisTemplate();
        template.setConnectionFactory(factoryNginx());
        return template;
    }

    @Bean(name = "redisTemplateReport")
    public StringRedisTemplate redisTemplateReport(){
        StringRedisTemplate template = getRedisTemplate();
        template.setConnectionFactory(factoryReport());
        return template;
    }



    private StringRedisTemplate getRedisTemplate(){
        StringRedisTemplate template = new StringRedisTemplate();
        template.setValueSerializer(new GenericFastJsonRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }

    private LettuceConnectionFactory createLettuceConnectionFactory(
            LettuceClientConfiguration clientConfiguration,RedisProperties redisProperties) {
        if (getSentinelConfig(redisProperties) != null) {
            return new LettuceConnectionFactory(getSentinelConfig(redisProperties), clientConfiguration);
        }
        if (getClusterConfiguration(redisProperties) != null) {
            return new LettuceConnectionFactory(getClusterConfiguration(redisProperties),
                    clientConfiguration);
        }
        return new LettuceConnectionFactory(getStandaloneConfig(redisProperties), clientConfiguration);
    }

    /**
     * Create a {@link RedisClusterConfiguration} if necessary.
     * @return {@literal null} if no cluster settings are set.
     */
    protected final RedisClusterConfiguration getClusterConfiguration(RedisProperties redisProperties) {
        if (redisProperties.getCluster() == null) {
            return null;
        }
        org.springframework.boot.autoconfigure.data.redis.RedisProperties.Cluster clusterProperties = redisProperties.getCluster();
        RedisClusterConfiguration config = new RedisClusterConfiguration(
                clusterProperties.getNodes());
        if (clusterProperties.getMaxRedirects() != null) {
            config.setMaxRedirects(clusterProperties.getMaxRedirects());
        }
        if (redisProperties.getPassword() != null) {
            config.setPassword(RedisPassword.of(redisProperties.getPassword()));
        }
        return config;
    }

    protected final RedisStandaloneConfiguration getStandaloneConfig(RedisProperties redisProperties) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName(redisProperties.getHost());
            config.setPort(redisProperties.getPort());
            config.setPassword(RedisPassword.of(redisProperties.getPassword()));

        config.setDatabase(redisProperties.getDatabase());
        return config;
    }

    private List<RedisNode> createSentinels(org.springframework.boot.autoconfigure.data.redis.RedisProperties.Sentinel sentinel) {
        List<RedisNode> nodes = new ArrayList<>();
        for (String node : sentinel.getNodes()) {
            try {
                String[] parts = StringUtils.split(node, ":");
                Assert.state(parts.length == 2, "Must be defined as 'host:port'");
                nodes.add(new RedisNode(parts[0], Integer.valueOf(parts[1])));
            }
            catch (RuntimeException ex) {
                throw new IllegalStateException(
                        "Invalid redis sentinel " + "property '" + node + "'", ex);
            }
        }
        return nodes;
    }

    protected final RedisSentinelConfiguration getSentinelConfig(RedisProperties redisProperties) {

        org.springframework.boot.autoconfigure.data.redis.RedisProperties.Sentinel sentinelProperties = redisProperties.getSentinel();
        if (sentinelProperties != null) {
            RedisSentinelConfiguration config = new RedisSentinelConfiguration();
            config.master(sentinelProperties.getMaster());
            config.setSentinels(createSentinels(sentinelProperties));
            if (redisProperties.getPassword() != null) {
                config.setPassword(RedisPassword.of(redisProperties.getPassword()));
            }
            config.setDatabase(redisProperties.getDatabase());
            return config;
        }
        return null;
    }

    private LettuceClientConfiguration.LettuceClientConfigurationBuilder applyProperties(
            LettuceClientConfiguration.LettuceClientConfigurationBuilder builder,RedisProperties redisProperties) {
        if (redisProperties.isSsl()) {
            builder.useSsl();
        }
        if (redisProperties.getTimeout() != null) {
            builder.commandTimeout(redisProperties.getTimeout());
        }
        if (redisProperties.getLettuce() != null) {
            org.springframework.boot.autoconfigure.data.redis.RedisProperties.Lettuce lettuce = redisProperties.getLettuce();
            if (lettuce.getShutdownTimeout() != null
                    && !lettuce.getShutdownTimeout().isZero()) {
                builder.shutdownTimeout(
                        redisProperties.getLettuce().getShutdownTimeout());
            }
        }
        return builder;
    }


}

