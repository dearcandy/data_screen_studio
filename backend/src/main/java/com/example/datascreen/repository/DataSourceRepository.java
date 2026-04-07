package com.example.datascreen.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.datascreen.entity.DataSourceEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DataSourceRepository extends BaseMapper<DataSourceEntity> {
}
