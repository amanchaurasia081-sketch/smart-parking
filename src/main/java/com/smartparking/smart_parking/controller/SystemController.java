package com.smartparking.smart_parking.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SystemController {

    @GetMapping("/time")
    public ResponseEntity<Map<String, String>> getServerTime() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of("now", Instant.now().toString()));
    }
}
