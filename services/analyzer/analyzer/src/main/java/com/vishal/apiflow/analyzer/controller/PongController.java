package com.vishal.apiflow.analyzer.controller;

import com.vishal.apiflow.analyzer.service.AlertPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.logging.FileHandler;

@RestController
public class PongController {
    
    @GetMapping("/ping")
    public String ping() {
        return "analyzer pong";
    }

}
