package com.example.datascreen.service;

import com.example.datascreen.dto.DataSetRequest;
import com.example.datascreen.entity.DataSetEntity;
import com.example.datascreen.entity.DataSourceEntity;
import com.example.datascreen.model.FetchMode;
import com.example.datascreen.repository.DataSetRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.datascreen.service.source.DataSourceHandlerManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;


/**
 * 数据集 CRUD 服务。
 */
@Service
@RequiredArgsConstructor
public class DataSetService {

    private final DataSetRepository dataSetRepository;
    private final ObjectMapper objectMapper;
    private final DataSourceHandlerManager handlerManager;
    private final DataSourceService dataSourceService;
    private final ScriptService scriptService;


    /**
     * 查询全部数据集
     * @return 全部数据集
     */
    public List<DataSetEntity> list() {
        return dataSetRepository.selectList(new LambdaQueryWrapper<>());
    }

    /**
     * 按主键查询数据集，不存在则抛异常。
     */
    public DataSetEntity get(Long id) {
        DataSetEntity entity = dataSetRepository.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("数据集不存在");
        }
        return entity;
    }

    /**
     * 新建数据集
     */
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
        dataSetRepository.insert(e);
        return e;
    }

    /**
     * 更新指定数据集
     */
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
        dataSetRepository.updateById(e);
        return e;
    }

    /**
     * 删除指定数据集
     */
    @Transactional
    public void delete(Long id) {
        dataSetRepository.deleteById(id);
    }

    /**
     * 重新生成 public token，旧 token 立即失效
     */
    @Transactional
    public DataSetEntity regenerateToken(Long id) {
        DataSetEntity e = get(id);
        e.setPublicToken(java.util.UUID.randomUUID().toString().replace("-", ""));
        e.setUpdatedAt(Instant.now());
        dataSetRepository.updateById(e);
        return e;
    }

    /**
     * 按数据集配置执行原始取数，不做脚本处理
     * @param source 数据源
     * @param dataSet 数据集
     * @return 数据对象
     * @throws Exception 对象处理异常
     */
    public Object fetchData(DataSourceEntity source, DataSetEntity dataSet) throws Exception {
        if (dataSet.getFetchMode() == FetchMode.MOCK) {
            if (dataSet.getMockJson() == null || dataSet.getMockJson().isBlank()) {
                return objectMapper.createArrayNode();
            }
            return objectMapper.readValue(dataSet.getMockJson(), Object.class);
        }
        if (source == null) {
            throw new IllegalArgumentException("LIVE 模式需要绑定数据源");
        }
        JsonNode cfg = objectMapper.readTree(source.getConfigJson());
        String spec = dataSet.getFetchSpec() != null ? dataSet.getFetchSpec().trim() : "";
        return handlerManager.get(source.getType()).fetch(cfg, spec);
    }


    /**
     * 按数据集 ID 执行
     */
    @Transactional(readOnly = true)
    public Object executeById(Long id) throws Exception {
        DataSetEntity ds = dataSetRepository.selectById(id);
        if (ds == null) {
            throw new IllegalArgumentException("数据集不存在");
        }
        return execute(ds);
    }

    /** 按公开 token 执行，用于嵌入接口。 */
    @Transactional(readOnly = true)
    public Object executeByToken(String token) throws Exception {
        DataSetEntity ds = dataSetRepository.findByPublicToken(token);
        if (ds == null) {
            throw new IllegalArgumentException("无效 token");
        }
        if (!ds.isEnabled()) {
            throw new IllegalStateException("数据集已禁用");
        }
        return execute(ds);
    }

    /** 内部统一执行流程：校验 -> 拉取原始数据 -> 脚本转换。 */
    private Object execute(DataSetEntity dataSetEntity) throws Exception {
        DataSourceEntity source = null;
        if (dataSetEntity.getFetchMode() == FetchMode.LIVE && dataSetEntity.getDataSourceId() != null) {
            source = dataSourceService.get(dataSetEntity.getDataSourceId());
            if (source == null) {
                throw new IllegalArgumentException("数据源不存在");
            }
        }
        if (dataSetEntity.getFetchMode() == FetchMode.LIVE && dataSetEntity.getDataSourceId() == null) {
            throw new IllegalArgumentException("LIVE 模式未绑定数据源");
        }
        Object data = fetchData(source, dataSetEntity);
        return scriptService.run(data, dataSetEntity.getScriptText());
    }
}
