package com.example.datascreen.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.datascreen.entity.PipelineEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PipelineRepository extends BaseMapper<PipelineEntity> {

    @Select("select * from ds_pipeline where public_token = #{publicToken} limit 1")
    PipelineEntity findByPublicToken(String publicToken);
}
