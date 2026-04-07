package com.example.datascreen.web;

import com.example.datascreen.dto.ApiResponse;
import com.example.datascreen.dto.DataSourceRequest;
import com.example.datascreen.entity.DataSourceEntity;
import com.example.datascreen.service.DataSourceCrudService;
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
 * 数据源管理：大屏侧各类连接的元数据 CRUD（配置以 JSON 存储）。
 */
@RestController
@RequestMapping("/api/datasources")
public class DataSourceController {

    private final DataSourceCrudService service;

    public DataSourceController(DataSourceCrudService service) {
        this.service = service;
    }

    /** 列出全部数据源。 */
    @GetMapping
    public ApiResponse<List<DataSourceEntity>> list() {
        return ApiResponse.ok(service.list());
    }

    /** 按主键查询。 */
    @GetMapping("/{id}")
    public ApiResponse<DataSourceEntity> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    /** 新建数据源。 */
    @PostMapping
    public ApiResponse<DataSourceEntity> create(@Valid @RequestBody DataSourceRequest req) {
        return ApiResponse.ok(service.create(req));
    }

    /** 更新指定数据源。 */
    @PutMapping("/{id}")
    public ApiResponse<DataSourceEntity> update(@PathVariable Long id, @Valid @RequestBody DataSourceRequest req) {
        return ApiResponse.ok(service.update(id, req));
    }

    /** 删除数据源。 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }
}
