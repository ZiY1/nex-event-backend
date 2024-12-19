package io.github.ziy1.nexevent.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExecutorConfig {

  @Bean
  public ExecutorService executorService(
      @Value("${executor.core.pool.size:1}") int corePoolSize,
      @Value("${executor.thread.pool.size:2}") int threadPoolSize,
      @Value("${executor.keep.alive.time:60}") long keepAliveTime,
      @Value("${executor.queue.size:10}") int queueSize) {
    return new ThreadPoolExecutor(
        corePoolSize,
        threadPoolSize,
        keepAliveTime,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(queueSize),
        new ThreadPoolExecutor.CallerRunsPolicy());
  }
}
