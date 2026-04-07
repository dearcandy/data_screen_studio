package com.example.datascreen.service;

import com.example.datascreen.dto.DataSetRequest;
import com.example.datascreen.entity.DataSetEntity;
import com.example.datascreen.repository.DataSetRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;


/**
 * 数据集 CRUD 服务。
 */
@Service
public class DataSetCrudService {

    private final DataSetRepository repository;

    public DataSetCrudService(DataSetRepository repository) {
        this.repository = repository;
    }

    /** 查询全部数据集。 */
    public List<DataSetEntity> list() {
        return repository.selectList(new LambdaQueryWrapper<>());
    }

    /** 按主键查询数据集，不存在则抛异常。 */
    public DataSetEntity get(Long id) {
        DataSetEntity entity = repository.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("数据集不存在");
        }
        return entity;
    }

    /** 新建数据集并初始化时间字段。 */
    @Transactional
    public DataSetEntity create(DataSetRequest req) {
        DataSetEntity e = new DataSetEntity();
        e.setName(req.getName());
        e.setDataSourceId(req.getDataSourceId());
        e.setFetchMode(req.getFetchMode());
        e.setFetchSpec(req.getFetchSpec());
        e.setMockJson(req.getMockJson());
        e.setScriptText(req.getScriptText());
        e.setEnabled(req.isEnabled());
        e.setCreatedAt(Instant.now());
        e.setUpdatedAt(Instant.now());
        repository.insert(e);
        return e;
    }

    /** 更新指定数据集。 */
    @Transactional
    public DataSetEntity update(Long id, DataSetRequest req) {
        DataSetEntity e = get(id);
        e.setName(req.getName());
        e.setDataSourceId(req.getDataSourceId());
        e.setFetchMode(req.getFetchMode());
        e.setFetchSpec(req.getFetchSpec());
        e.setMockJson(req.getMockJson());
        e.setScriptText(req.getScriptText());
        e.setEnabled(req.isEnabled());
        e.setUpdatedAt(Instant.now());
        repository.updateById(e);
        return e;
    }

    /** 删除指定数据集。 */
    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    /** 重新生成 public token，旧 token 立即失效。 */
    @Transactional
    public DataSetEntity regenerateToken(Long id) {
        DataSetEntity e = get(id);
        e.setPublicToken(java.util.UUID.randomUUID().toString().replace("-", ""));
        e.setUpdatedAt(Instant.now());
        repository.updateById(e);
        return e;
    }
}
