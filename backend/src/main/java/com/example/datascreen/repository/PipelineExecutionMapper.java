package com.example.datascreen.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.datascreen.entity.PipelineExecutionRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PipelineExecutionMapper extends BaseMapper<PipelineExecutionRecord> {
    // 可以扩展自定义查询方法，例如：
    // List<PipelineExecutionRecord> selectByPipelineId(@Param("pipelineId") Long pipelineId, Page page);
}