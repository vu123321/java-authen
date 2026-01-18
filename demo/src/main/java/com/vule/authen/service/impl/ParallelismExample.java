package com.vule.authen.service.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.stream.LongStream;

public class ParallelismExample {

    private static double heavyComputation(long n) {
        double result = 0;
        for (long i = 1; i <= n; i++) {
            result += Math.sqrt(i);
        }
        return result;
    }

    public static void main(String[] args) {
        int taskCount = 8;
        var n = 5_000_000; // tăng lên nếu máy bạn khoẻ để thấy rõ

        System.out.println("Running with parallelStream (parallelism)...");

        long start = System.currentTimeMillis();

        BigDecimal sum = BigDecimal.valueOf(LongStream.rangeClosed(1, taskCount)
                .parallel() // <--- song song trên nhiều core
                .mapToDouble(i -> heavyComputation(n))
                .sum());

        long end = System.currentTimeMillis();

        System.out.println("Result = " + sum);
        System.out.println("Time   = " + (end - start) + " ms");
    }
}
