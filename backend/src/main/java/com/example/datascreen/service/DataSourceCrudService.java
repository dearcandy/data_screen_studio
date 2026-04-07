package com.example.datascreen.service;

import com.example.datascreen.dto.DataSourceRequest;
import com.example.datascreen.entity.DataSourceEntity;
import com.example.datascreen.repository.DataSourceRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;


/**
 * 数据源 CRUD 服务。
 */
@Service
public class DataSourceCrudService {

    private final DataSourceRepository repository;
    private final DataSourceConfigValidator configValidator;

    public DataSourceCrudService(DataSourceRepository repository, DataSourceConfigValidator configValidator) {
        this.repository = repository;
        this.configValidator = configValidator;
    }

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
        configValidator.validate(req.getType(), req.getConfigJson());
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
        configValidator.validate(req.getType(), req.getConfigJson());
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
}
