package com.example.datascreen.web;

import com.example.datascreen.dto.ApiResponse;
import com.example.datascreen.dto.ConnectionTestRequest;
import com.example.datascreen.dto.DataSourceRequest;
import com.example.datascreen.entity.DataSourceEntity;
import com.example.datascreen.service.DataSourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
 * 数据源管理
 */
@RestController
@RequestMapping("/api/datasources")
@RequiredArgsConstructor
public class DataSourceController {

    private final DataSourceService dataSourceService;

    /**
     * 列出全部数据源
     *
     * @return 数据源列表
     */
    @GetMapping
    public ApiResponse<List<DataSourceEntity>> list() {
        return ApiResponse.ok(dataSourceService.list());
    }

    /**
     * 按主键查询数据源
     *
     * @param id 数据源ID
     * @return 数据源
     */
    @GetMapping("/{id}")
    public ApiResponse<DataSourceEntity> get(@PathVariable Long id) {
        return ApiResponse.ok(dataSourceService.get(id));
    }

    /**
     * 新建数据源
     *
     * @param req 创建请求
     * @return 数据源
     */
    @PostMapping
    public ApiResponse<DataSourceEntity> create(@Valid @RequestBody DataSourceRequest req) {
        return ApiResponse.ok(dataSourceService.create(req));
    }

    /**
     * 更新指定数据源
     *
     * @param id  数据源ID
     * @param req 更新请求
     * @return 数据源
     */
    @PutMapping("/{id}")
    public ApiResponse<DataSourceEntity> update(@PathVariable Long id, @Valid @RequestBody DataSourceRequest req) {
        return ApiResponse.ok(dataSourceService.update(id, req));
    }

    /**
     * 删除数据源
     *
     * @param id 数据源ID
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        dataSourceService.delete(id);
        return ApiResponse.ok(null);
    }

    /**
     * 测试数据源连接
     *
     * @param connectionTestRequest 测试请求
     * @return 测试结果
     * @throws Exception 测试异常
     */
    @PostMapping("/test")
    public ApiResponse<Void> test(@Valid @RequestBody ConnectionTestRequest connectionTestRequest) throws Exception {
        dataSourceService.test(connectionTestRequest);

        return ApiResponse.ok(null);
    }
}
