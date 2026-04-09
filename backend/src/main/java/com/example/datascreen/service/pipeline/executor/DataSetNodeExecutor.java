package com.example.datascreen.service.pipeline.executor;

import com.example.datascreen.model.Node;
import com.example.datascreen.service.DataSetService;
import com.example.datascreen.service.pipeline.PipelineContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Map;


@Slf4j
@Component
@RequiredArgsConstructor
public class DataSetNodeExecutor extends AbstractNodeExecutor {

    private final DataSetService dataSetService;

    @Override
    public boolean supports(String nodeType) {
        return "dataSet".equals(nodeType);
    }

    @Override
    protected Object doExecute(Node node, PipelineContext context, Map<String, Object> params) throws Exception {
        Long dataSetId = node.getConfigLong("dataSetId");
        if (dataSetId == null) {
            throw new IllegalArgumentException("数据集节点必须配置 dataSetId");
        }
        Object result = dataSetService.executeById(dataSetId);
        log.info("数据集 {} 执行完成，结果是否为空: {}", dataSetId, result == null ? "是" : "否");
        return result;
    }

}