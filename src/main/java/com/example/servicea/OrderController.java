package com.example.servicea;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@RestController
public class OrderController {

    private final RestTemplate rest = new RestTemplate();

    @GetMapping("/order/{id}")
    public Map<String, Object> order(@PathVariable String id) {
        String payment = rest.getForObject("http://localhost:8082/payment/" + id, String.class);
        return Map.of(
                "service", "A",
                "payment", payment
        );
    }
}
