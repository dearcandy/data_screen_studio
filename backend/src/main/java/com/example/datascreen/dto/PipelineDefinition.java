package com.example.datascreen.dto;

import com.example.datascreen.model.Node;
import lombok.Data;
import java.util.List;

@Data
public class PipelineDefinition {
    private List<Node> nodes;
    // 可扩展其他元数据
}