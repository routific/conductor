package com.netflix.conductor.contribs.kafka;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.netflix.conductor.core.config.Configuration;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class KafkaProducerManager {

	public static final String KAFKA_PUBLISH_REQUEST_TIMEOUT_MS = "kafka.publish.request.timeout.ms";
	public static final String STRING_SERIALIZER = "org.apache.kafka.common.serialization.StringSerializer";
	public static final String DEFAULT_REQUEST_TIMEOUT = "100";
	private static final String KAFKA_PRODUCER_CACHE_TIME_IN_MILLIS = "kafka.publish.producer.cache.time.ms" ;
	private static final int DEFAULT_CACHE_SIZE = 10;
	private static final String KAFKA_PRODUCER_CACHE_SIZE = "kafka.publish.producer.cache.size";
	private static final int DEFAULT_CACHE_TIME_IN_MILLIS = 120000;

	private static final Logger logger = LoggerFactory.getLogger(KafkaProducerManager.class);

	public static final RemovalListener<Properties, Producer> LISTENER = new RemovalListener<Properties, Producer>() {
		@Override
		public void onRemoval(RemovalNotification<Properties, Producer> notification) {
			notification.getValue().close();
			logger.info("Closed producer for {}",notification.getKey());
		}
	};
	private static final String KAFKA_PUBLISH_MAX_BLOCK_MS = "kafka.publish.max.block.ms";
	private static final String DEFAULT_MAX_BLOCK_MS = "500";

	private static final String KAFKA_PUBLISH_SECURITY_PROTOCOL = "kafka.publish.security.protocol";
	private static final String DEFAULT_SECURITY_PROTOCOL = null;

	private static final String KAFKA_PUBLISH_SASL_MECHANISM = "kafka.publish.sasl.mechanism";
	private static final String DEFAULT_SASL_MECHANISM = "PLAIN";

	private static final String KAFKA_PUBLISH_SASL_USERNAME = "kafka.publish.sasl.username";
	private static final String KAFKA_PUBLISH_SASL_PASSWORD = "kafka.publish.sasl.password";

	public final String requestTimeoutConfig;
	private Cache<Properties, Producer> kafkaProducerCache;
	private final String maxBlockMsConfig;

	private final String securityProtocolConfig;
	private final String saslMechanismConfig;
	private final String saslUsernameConfig;
	private final String saslPasswordConfig;

	public KafkaProducerManager(Configuration configuration) {
		this.requestTimeoutConfig = configuration.getProperty(KAFKA_PUBLISH_REQUEST_TIMEOUT_MS, DEFAULT_REQUEST_TIMEOUT);
		this.maxBlockMsConfig = configuration.getProperty(KAFKA_PUBLISH_MAX_BLOCK_MS, DEFAULT_MAX_BLOCK_MS);
		this.securityProtocolConfig = configuration.getProperty(KAFKA_PUBLISH_SECURITY_PROTOCOL, DEFAULT_SECURITY_PROTOCOL);
		this.saslMechanismConfig = configuration.getProperty(KAFKA_PUBLISH_SASL_MECHANISM, DEFAULT_SASL_MECHANISM);
		this.saslUsernameConfig = configuration.getProperty(KAFKA_PUBLISH_SASL_USERNAME, "");
		this.saslPasswordConfig = configuration.getProperty(KAFKA_PUBLISH_SASL_PASSWORD, "");

		int cacheSize = configuration.getIntProperty(KAFKA_PRODUCER_CACHE_SIZE, DEFAULT_CACHE_SIZE);
		int cacheTimeInMs = configuration.getIntProperty(KAFKA_PRODUCER_CACHE_TIME_IN_MILLIS, DEFAULT_CACHE_TIME_IN_MILLIS);
		this.kafkaProducerCache = CacheBuilder.newBuilder().removalListener(LISTENER)
				.maximumSize(cacheSize).expireAfterAccess(cacheTimeInMs, TimeUnit.MILLISECONDS)
				.build();
	}


	public Producer getProducer(KafkaPublishTask.Input input) {

		Properties configProperties = getProducerProperties(input);

		return getFromCache(configProperties, () -> new KafkaProducer(configProperties));

	}

	@VisibleForTesting
	Producer getFromCache(Properties configProperties, Callable<Producer> createProducerCallable) {
		try {
			return kafkaProducerCache.get(configProperties, createProducerCallable);
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	@VisibleForTesting
	Properties getProducerProperties(KafkaPublishTask.Input input) {

		Properties configProperties = new Properties();
		configProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, input.getBootStrapServers());

		configProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, input.getKeySerializer());

		String requestTimeoutMs = requestTimeoutConfig;

		if (Objects.nonNull(input.getRequestTimeoutMs())) {
			requestTimeoutMs = String.valueOf(input.getRequestTimeoutMs());
		}

		String maxBlockMs = maxBlockMsConfig;

		if (Objects.nonNull(input.getMaxBlockMs())) {
			maxBlockMs = String.valueOf(input.getMaxBlockMs());
		}

		configProperties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, requestTimeoutMs);
		configProperties.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, maxBlockMs);
		configProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, STRING_SERIALIZER);

		if (Objects.nonNull(securityProtocolConfig)) {
			configProperties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocolConfig);
			configProperties.put(SaslConfigs.SASL_MECHANISM, saslMechanismConfig);
			configProperties.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\""+saslUsernameConfig+"\" password=\""+saslPasswordConfig+"\";");
		}

		return configProperties;
	}
}
