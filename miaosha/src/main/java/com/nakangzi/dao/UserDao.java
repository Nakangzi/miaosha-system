package com.nakangzi.dao;

import com.nakangzi.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserDao {
    User findById(Integer id);
}
