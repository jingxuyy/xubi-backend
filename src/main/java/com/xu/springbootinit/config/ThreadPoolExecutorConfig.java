package com.xu.springbootinit.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author: xuJing
 * @date: 2024/4/7 19:43
 */

@Configuration
public class ThreadPoolExecutorConfig {



    @Bean
    public ThreadPoolExecutor threadPoolExecutor(){

        // 自定义线程工厂
        ThreadFactory threadFactory = new ThreadFactory() {
            private int count = 1;

            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("线程：" + count);
                count++;
                return thread;
            }
        };

        // 创建线程池
        // 核心线程数2
        // 最大线程数4
        // 存活时间120秒
        // 容量为100的数组阻塞队列
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(2,
                4,
                120,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100),
                threadFactory);
        return threadPoolExecutor;
    }
}
