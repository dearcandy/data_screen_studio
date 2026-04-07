package com.example.datascreen.service;

import com.example.datascreen.entity.DataSetEntity;
import com.example.datascreen.entity.DataSourceEntity;
import com.example.datascreen.model.FetchMode;
import com.example.datascreen.repository.DataSetRepository;
import com.example.datascreen.repository.DataSourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * 数据集执行服务：串联取数与脚本处理。
 */
@Service
public class DataSetExecutionService {

    private final DataSetRepository dataSetRepository;
    private final DataSourceRepository dataSourceRepository;
    private final DataFetchService dataFetchService;
    private final ScriptService scriptService;

    public DataSetExecutionService(
            DataSetRepository dataSetRepository,
            DataSourceRepository dataSourceRepository,
            DataFetchService dataFetchService,
            ScriptService scriptService) {
        this.dataSetRepository = dataSetRepository;
        this.dataSourceRepository = dataSourceRepository;
        this.dataFetchService = dataFetchService;
        this.scriptService = scriptService;
    }

    /** 按数据集 ID 执行。 */
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
    private Object execute(DataSetEntity ds) throws Exception {
        DataSourceEntity source = null;
        if (ds.getFetchMode() == FetchMode.LIVE && ds.getDataSourceId() != null) {
            source = dataSourceRepository.selectById(ds.getDataSourceId());
            if (source == null) {
                throw new IllegalArgumentException("数据源不存在");
            }
        }
        if (ds.getFetchMode() == FetchMode.LIVE && ds.getDataSourceId() == null) {
            throw new IllegalArgumentException("LIVE 模式未绑定数据源");
        }
        Object raw = dataFetchService.fetchRaw(source, ds);
        return scriptService.run(raw, ds.getScriptText());
    }
}
