package com.example.datascreen.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.datascreen.entity.DataSetEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DataSetRepository extends BaseMapper<DataSetEntity> {

    @Select("select * from ds_data_set where public_token = #{publicToken} limit 1")
    DataSetEntity findByPublicToken(String publicToken);
}
