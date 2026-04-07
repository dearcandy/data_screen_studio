package com.example.datascreen;

import com.example.datascreen.service.ScriptService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class DataScreenStudioApplicationTests {

    @Autowired
    ScriptService scriptService;

    @Test
    void scriptFiltersArray() throws Exception {
        List<Map<String, Object>> input = List.of(
                Map.of("v", 1),
                Map.of("v", 5)
        );
        Object out = scriptService.run(input, "return input.filter(x => x.v > 2);");
        assertEquals(1, ((List<?>) out).size());
    }
}
