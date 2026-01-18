package com.vule.authen.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SequentialVsParallel {

    private static long heavyCpuTask(int n) {
        long result = 0;
        for (int i = 1; i <= 200_000; i++) {
            result += (long) Math.sqrt(i + n);
        }
        return result;
    }

    public static void main(String[] args) {
        List<Integer> tasks = IntStream.rangeClosed(1, 8)
                .boxed()
                .toList();

        // Sequential
        long startSeq = System.currentTimeMillis();
        long sumSeq = tasks.stream()
                .mapToLong(SequentialVsParallel::heavyCpuTask)
                .sum();
        long endSeq = System.currentTimeMillis();
        System.out.println("Sequential result = " + sumSeq);
        System.out.println("Sequential time   = " + (endSeq - startSeq) + " ms");

        // Parallel
        long startPar = System.currentTimeMillis();
        long sumPar = tasks.parallelStream()
                .mapToLong(SequentialVsParallel::heavyCpuTask)
                .sum();
        long endPar = System.currentTimeMillis();
        System.out.println("Parallel   result = " + sumPar);
        System.out.println("Parallel   time   = " + (endPar - startPar) + " ms");
    }
}

