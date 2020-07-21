package com.nakangzi.controller;

import com.google.common.util.concurrent.RateLimiter;
import com.nakangzi.service.OrderService;
import com.nakangzi.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("stock")
@Slf4j
public class StockController {

    @Autowired
    private OrderService orderService;

    //创建令牌桶实例
    private RateLimiter rateLimiter=RateLimiter.create(40);

    @Autowired
    private UserService userService;

    //生成md5值的方法
    @RequestMapping("md5")
    public String getMd5(Integer id,Integer userid){
        String md5;
        try{
            md5=orderService.getMd5(id,userid);
        }catch(Exception e){
            e.printStackTrace();
            return "获取md5失败："+e.getMessage();
        }
        return "获取md5信息为："+md5;
    }

    //乐观锁防止超卖
    @GetMapping("kill")
    public String kill(Integer id){
        System.out.println(("秒杀商品的id=" + id));
        try {
            //根据秒杀商品id去调用秒杀业务
            int orderId = orderService.kill(id);
            return "秒杀成功，订单方法为" + String.valueOf(orderId);
        }catch(Exception e){
            e.printStackTrace();
            return e.getMessage();
        }
    }

    //开发一个秒杀方法，乐观锁防止超卖+令牌桶算法限流
    @GetMapping("killtokenmd5")
    public String killtoken(Integer id,Integer userid,String md5){
        System.out.println(("秒杀商品的id=" + id));
        if (!rateLimiter.tryAcquire(2, TimeUnit.SECONDS)){
            log.info("抛弃请求：抢购失败，当前秒杀活动过于火爆，请重试");
            return "抢购失败，当前秒杀系统过于火爆，请重试";
        }
        try {
            //根据秒杀商品id去调用秒杀业务
            int orderId = orderService.kill(id,userid,md5);
            return "秒杀成功，订单方法为" + String.valueOf(orderId);
        }catch(Exception e){
            e.printStackTrace();
            return e.getMessage();
        }
    }

    //开发一个秒杀方法，乐观锁防止超卖+令牌桶算法限流+单用户访问频率限制
    @GetMapping("killtokenmd5limit")
    public String killtokenlimit(Integer id,Integer userid,String md5){
        if (!rateLimiter.tryAcquire(3, TimeUnit.SECONDS)){
            log.info("抛弃请求：抢购失败，当前秒杀活动过于火爆，请重试");
            return "抢购失败，当前秒杀系统过于火爆，请重试";
        }
        try {
            //单用户调用接口的频率限制
            int count=userService.saveUserCount(userid);
            log.info("用户截至该次访问的次数为：[{}]",count);

            //进行调用次数的判断
            boolean isBanned = userService.getUserCount(userid);
            if(isBanned){
                log.info("购买失败，超过频率限制");
                return "购买失败，超过频率限制";
            }

            //根据秒杀商品id去调用秒杀业务
            int orderId = orderService.kill(id,userid,md5);
            return "秒杀成功，订单方法为" + String.valueOf(orderId);
        }catch(Exception e){
            e.printStackTrace();
            return e.getMessage();
        }
    }
}
