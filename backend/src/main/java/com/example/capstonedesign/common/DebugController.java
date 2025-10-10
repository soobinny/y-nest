package com.example.capstonedesign.common;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class DebugController {
    private final DataSource dataSource;

    @GetMapping("/admin/debug/datasource")
    public Map<String, String> ds() throws Exception {
        try (Connection c = dataSource.getConnection()) {
            return Map.of(
                    "url", c.getMetaData().getURL(),
                    "user", c.getMetaData().getUserName(),
                    "catalog(database)", c.getCatalog()
            );
        }
    }
}
