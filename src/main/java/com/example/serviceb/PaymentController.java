package com.example.serviceb;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@RestController
public class PaymentController {

    private final RestTemplate rest = new RestTemplate();

    @GetMapping("/payment/{id}")
    public Map<String, Object> payment(@PathVariable String id) {
        String db = rest.getForObject("http://localhost:8083/db/" + id, String.class);
        return Map.of(
                "service", "B",
                "db", db
        );
    }
}
