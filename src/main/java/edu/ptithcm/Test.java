package edu.ptithcm;

import java.util.concurrent.CompletableFuture;

public class Test {
    static CompletableFuture<Integer> calcSum(int a, int b){
        CompletableFuture<Integer> res = new CompletableFuture<>();
        Thread t = new Thread(()->{
            try {
                Thread.sleep(5000); // 5 second
                res.complete(a +  b);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        t.start();

        return res;
    }
    static void main(){
        IO.println("Test CompleableFuture<T> ");
        CompletableFuture<Integer> future = calcSum(10, 20);
        future.thenAccept(result->IO.println("Kê quá tính toán:"+result));
        IO.println("Thấy chưa, việc tính toán dù delay 5s luồng chính vẫn không bị chặn");
    }
}
