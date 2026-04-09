package com.example.datascreen.web;

import com.example.datascreen.dto.ApiResponse;
import com.example.datascreen.dto.DataSetRequest;
import com.example.datascreen.entity.DataSetEntity;
import com.example.datascreen.service.DataSetService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据集管理：绑定数据源或 Mock、配置取数说明与脚本，并提供预览与公开 token 轮换。
 */
@RestController
@RequestMapping("/api/datasets")
public class DataSetController {

    private final DataSetService dataSetService;

    public DataSetController(DataSetService dataSetService) {
        this.dataSetService = dataSetService;
    }

    /**
     * 列出全部数据集
     * @return 数据集列表
     */
    @GetMapping
    public ApiResponse<List<DataSetEntity>> list() {
        return ApiResponse.ok(dataSetService.list());
    }

    /**
     * 按主键查询
     * @param id 数据集ID
     * @return 数据集
     */
    @GetMapping("/{id}")
    public ApiResponse<DataSetEntity> get(@PathVariable Long id) {
        return ApiResponse.ok(dataSetService.get(id));
    }

    /**
     * 新建数据集
     * @param req 数据集
     * @return 数据集
     */
    @PostMapping
    public ApiResponse<DataSetEntity> create(@Valid @RequestBody DataSetRequest req) {
        return ApiResponse.ok(dataSetService.create(req));
    }

    /**
     * 更新指定数据集
     * @param id 数据集ID
     * @param req 数据集
     * @return 数据集
     */
    @PutMapping("/{id}")
    public ApiResponse<DataSetEntity> update(@PathVariable Long id, @Valid @RequestBody DataSetRequest req) {
        return ApiResponse.ok(dataSetService.update(id, req));
    }

    /**
     * 删除数据集
     * @param id 数据集ID
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        dataSetService.delete(id);
        return ApiResponse.ok(null);
    }

    /**
     * 执行完整链路：取数（或 Mock）+ 脚本，返回与嵌入接口一致的data形态
     * @param id 数据集ID
     * @return 预览数据
     */
    @PostMapping("/{id}/preview")
    public ApiResponse<Object> preview(@PathVariable Long id) throws Exception {
        Object data = dataSetService.executeById(id);
        return ApiResponse.ok(data);
    }

    /**
     *  重新生成 {@code publicToken}，旧嵌入 URL 失效
     * @param id 数据集ID
     * @return 数据集
     */
    @PostMapping("/{id}/regenerate-token")
    public ApiResponse<DataSetEntity> regenerateToken(@PathVariable Long id) {
        return ApiResponse.ok(dataSetService.regenerateToken(id));
    }
}
