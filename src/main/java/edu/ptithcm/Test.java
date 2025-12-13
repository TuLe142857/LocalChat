package edu.ptithcm;

import edu.ptithcm.bus.MessageBus;

import edu.ptithcm.cache.Cache;
import edu.ptithcm.model.*;
import edu.ptithcm.network.NetworkService;
import edu.ptithcm.security.CryptoUtils;
import edu.ptithcm.service.AuthService;
import edu.ptithcm.service.ChatService;
import edu.ptithcm.util.JsonUtils;
import edu.ptithcm.util.LogConfig;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

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
