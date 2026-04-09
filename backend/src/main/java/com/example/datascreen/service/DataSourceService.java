package com.example.datascreen.service;

import com.example.datascreen.dto.ConnectionTestRequest;
import com.example.datascreen.dto.DataSourceRequest;
import com.example.datascreen.entity.DataSourceEntity;
import com.example.datascreen.model.SourceType;
import com.example.datascreen.repository.DataSourceRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.datascreen.service.source.DataSourceHandlerManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;


/**
 * 数据源 CRUD 服务。
 */
@Service
@RequiredArgsConstructor
public class DataSourceService {

    private final DataSourceRepository repository;
    private final ObjectMapper objectMapper;
    private final DataSourceHandlerManager dataSourceHandlerManager;


    /** 查询全部数据源。 */
    public List<DataSourceEntity> list() {
        return repository.selectList(new LambdaQueryWrapper<>());
    }

    /** 按主键查询数据源，不存在则抛异常。 */
    public DataSourceEntity get(Long id) {
        DataSourceEntity entity = repository.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("数据源不存在");
        }
        return entity;
    }

    /** 新建数据源。 */
    @Transactional
    public DataSourceEntity create(DataSourceRequest req) {
        this.validate(req.getType(), req.getConfigJson());
        DataSourceEntity e = new DataSourceEntity();
        e.setName(req.getName());
        e.setType(req.getType());
        e.setConfigJson(req.getConfigJson());
        e.setRemark(req.getRemark());
        e.setCreatedAt(Instant.now());
        e.setUpdatedAt(Instant.now());
        repository.insert(e);
        return e;
    }

    /** 更新指定数据源。 */
    @Transactional
    public DataSourceEntity update(Long id, DataSourceRequest req) {
        this.validate(req.getType(), req.getConfigJson());
        DataSourceEntity e = get(id);
        e.setName(req.getName());
        e.setType(req.getType());
        e.setConfigJson(req.getConfigJson());
        e.setRemark(req.getRemark());
        e.setUpdatedAt(Instant.now());
        repository.updateById(e);
        return e;
    }

    /** 删除指定数据源。 */
    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }


    /**
     * 测试数据源联通性
     */
    public void test(@Valid ConnectionTestRequest connectionTestRequest) throws Exception {
        // 校验数据源配置
        JsonNode cfg = this.validate(connectionTestRequest.getType(), connectionTestRequest.getConfigJson());
        // 测试数据源连接
        dataSourceHandlerManager.get(connectionTestRequest.getType()).testConnection(cfg);
    }

    public JsonNode validate(SourceType type, String configJson) {
        if (configJson == null || configJson.isBlank()) {
            throw new IllegalArgumentException("配置 JSON 不能为空");
        }
        try {
            JsonNode cfg = objectMapper.readTree(configJson);
            if (!cfg.isObject()) {
                throw new IllegalArgumentException("配置必须是 JSON 对象");
            }
            dataSourceHandlerManager.get(type).validateConfig(cfg);
            return cfg;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("配置 JSON 格式错误: " + e.getMessage());
        }
    }
}
