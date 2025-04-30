package org.hairo.server.endpoints;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorldEndpoint {

    @GetMapping("/hello")
    public String getHelloWorld() {
        return "Hello World";
    }

}
