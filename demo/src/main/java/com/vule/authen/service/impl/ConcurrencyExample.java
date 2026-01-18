package com.vule.authen.service.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ConcurrencyExample {

    public static void main(String[] args) throws InterruptedException {
        // Thread pool 3 thread
        ExecutorService executor = Executors.newFixedThreadPool(3);

        for (int i = 1; i <= 10; i++) {
            int taskId = i;
            executor.submit(() -> {
                String threadName = Thread.currentThread().getName();
                System.out.println("Start task " + taskId + " on " + threadName);
                try {
                    // Giả lập IO-bound: chờ API, chờ DB,...
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("End   task " + taskId + " on " + threadName);
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
        System.out.println("All tasks done (concurrency example).");
    }
}
