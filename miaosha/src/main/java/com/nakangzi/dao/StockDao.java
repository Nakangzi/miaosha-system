package com.nakangzi.dao;

import com.nakangzi.entity.Stock;

public interface StockDao {

    //根据商品id查询库存信息
    Stock checkStock(Integer id);

    //根据商品id扣除库存
    int updateSale(Stock stock);
}
