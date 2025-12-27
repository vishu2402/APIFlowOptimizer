package com.vishal.apiflow.analyzer.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GraphiQLController {
    @GetMapping("/graphiql")
    public String forwardGraphiql() {
        return "forward:/graphiql/index.html";
    }
}
