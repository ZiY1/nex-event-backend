package io.github.ziy1.nexevent.config;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Configuration
public class WebClientConfig {
  @Value("${webclient.max-connections:500}")
  private int maxConnections;

  @Value("${webclient.max-idle-time:20}")
  private int maxIdleTime;

  @Value("${webclient.max-life-time:5}")
  private int maxLifeTime;

  @Value("${webclient.pending-acquire-timeout:60}")
  private int pendingAcquireTimeout;

  @Value("${webclient.response-timeout:10}")
  private int responseTimeout;

  @Value("${webclient.connection-timeout:5000}")
  private int connectionTimeout;

  @Value("${webclient.max-in-memory-size:10485760}")
  private int maxInMemorySize;

  @Bean
  public WebClient webClient(WebClient.Builder builder) {
    // Connection pool configuration
    ConnectionProvider provider =
        ConnectionProvider.builder("custom")
            .maxConnections(maxConnections) // Maximum number of concurrent connections
            .maxIdleTime(Duration.ofSeconds(maxIdleTime)) // How long a connection can be idle
            .maxLifeTime(Duration.ofMinutes(maxLifeTime)) // Maximum lifetime of a connection
            .pendingAcquireTimeout(
                Duration.ofSeconds(pendingAcquireTimeout)) // How long to wait for a connection
            .build();

    // HTTP client configuration
    HttpClient httpClient =
        HttpClient.create(provider)
            .responseTimeout(
                Duration.ofSeconds(responseTimeout)) // Maximum time to wait for response
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout); // Connection timeout

    // Memory buffer configuration
    ExchangeStrategies strategies =
        ExchangeStrategies.builder()
            .codecs(
                configurer -> {
                  configurer.defaultCodecs().maxInMemorySize(maxInMemorySize); // 10MB buffer
                  configurer
                      .defaultCodecs()
                      .enableLoggingRequestDetails(true); // Enable detailed logging
                })
            .build();

    return builder
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .exchangeStrategies(strategies)
        .filter(ExchangeFilterFunction.ofRequestProcessor(Mono::just))
        .build();
  }
}
