package com.zr.health.ai.agent.queue;

import org.springframework.stereotype.Component;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 用户消息队列 采用 BlockingQueue 阻塞队列作
 */
@Component
public class UserInputQueue {
    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    public void putResponse(String response) throws InterruptedException {
        queue.put(response);
    }
    public String takeResponse() throws InterruptedException {
        return queue.take();
    }
}