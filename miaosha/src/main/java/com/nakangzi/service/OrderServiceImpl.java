package com.nakangzi.service;

import com.nakangzi.dao.OrderDao;
import com.nakangzi.dao.StockDao;
import com.nakangzi.dao.UserDao;
import com.nakangzi.entity.Order;
import com.nakangzi.entity.Stock;
import com.nakangzi.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private StockDao stockDao;

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserDao userDao;

    @Override
    public String getMd5(Integer id,Integer userid){
        //检查用户的合法性
        User user= UserDao.findById(userid);
        if(user==null)throw new RuntimeException("用户信息不存在！");
        log.info("用户信息：[{}]",user.toString());
        //检验商品的合法性
        Stock stock=stockDao.checkStock(id);
        if(stock==null)throw new RuntimeException("商品信息不合法！");
        log.info("商品信息：[{}]",stock.toString());
        //生成hashkey
        String hashKey="KEY_"+userid+"_"+id;
        //生成md5
        String key= DigestUtils.md5DigestAsHex((userid+id+"!Q*jS#").getBytes());
        stringRedisTemplate.opsForValue().set(hashKey,key,3600, TimeUnit.SECONDS);
        log.info("Redis写入：[{}] [{}]",hashKey,key);
        return key;
    }

    @Override
    public int kill(Integer id, Integer userid, String md5) {

        //校验redis中的秒杀商品是否超时
        if(!stringRedisTemplate.hasKey("kill"+id)){
            throw new RuntimeException("当前抢购活动已经结束~~");
        }
        //验证签名
        String hashKey="KEY_"+userid+"_"+id;
        String s=stringRedisTemplate.opsForValue().get(hashKey);
        if(s==null)throw new RuntimeException("没有携带验证签名，请求不合法");
        if(!s.equals(md5));
           throw new RuntimeException("当前请求数据不合法，请稍后再试");

        //校验库存
        Stock stock = checkStock(id);
        updateStock(stock);
        return createOrder(stock);
    }

    @Override
    public int kill(Integer id) {

        //校验redis中的秒杀商品是否超时
        if(!stringRedisTemplate.hasKey("kill"+id)){
            throw new RuntimeException("当前抢购活动已经结束~~");
        }
        //校验库存
        Stock stock=checkStock(id);
        updateStock(stock);
        return createOrder(stock);
    }


    //校验库存
    private Stock checkStock(Integer id){
        Stock stock=stockDao.checkStock(id);
        if(stock.getSale().equals(stock.getCount())){
            throw new RuntimeException("库存不足！");
        }
        return stock;
    }
    //扣除库存

    private void updateStock(Stock stock){
        //在sql层面完成销量+1 和 版本号+1，并且根据商品id和版本号同时查询
        int updateRows =stockDao.updateSale(stock);
        if(updateRows==0){
            throw new RuntimeException("抢购失败，请重试");
        }
    }
    //创建订单
    private Integer createOrder(Stock stock){
        Order order=new Order();
        order.setSid(stock.getId()).setName(stock.getName()).setCreateDate(new Date());
        orderDao.createOrder(order);
        return order.getId();
    }
}
