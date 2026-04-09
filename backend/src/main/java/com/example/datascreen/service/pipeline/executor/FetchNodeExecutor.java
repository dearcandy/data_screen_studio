package com.example.datascreen.service.pipeline.executor;

import com.example.datascreen.entity.DataSetEntity;
import com.example.datascreen.entity.DataSourceEntity;
import com.example.datascreen.model.FetchMode;
import com.example.datascreen.model.Node;
import com.example.datascreen.model.NodeType;
import com.example.datascreen.service.DataSetService;
import com.example.datascreen.service.DataSourceService;
import com.example.datascreen.service.pipeline.PipelineContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 该节点用于从数据源实时获取数据，不依赖于已保存的数据集。
 */
@Component
@RequiredArgsConstructor
public class FetchNodeExecutor extends AbstractNodeExecutor {

    private final DataSourceService dataSourceService;
    private final DataSetService dataSetService;

    @Override
    protected Object doExecute(Node node, PipelineContext context, Map<String, Object> params) throws Exception {
        Long dataSourceId = node.getConfigLong("dataSourceId");
        if (dataSourceId == null) {
            throw new IllegalArgumentException("取数节点必须配置 dataSourceId");
        }
        String fetchSpec = node.getConfigString("fetchSpec");
        if (fetchSpec == null || fetchSpec.isBlank()) {
            throw new IllegalArgumentException("取数节点必须配置 fetchSpec");
        }
        DataSourceEntity source = dataSourceService.get(dataSourceId);
        DataSetEntity temp = new DataSetEntity();
        temp.setFetchMode(FetchMode.LIVE);
        temp.setFetchSpec(fetchSpec);
        return dataSetService.fetchData(source, temp);
    }

    @Override
    public NodeType type() {
        return NodeType.FETCH;
    }
}
