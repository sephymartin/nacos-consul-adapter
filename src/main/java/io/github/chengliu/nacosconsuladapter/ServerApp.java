package io.github.chengliu.nacosconsuladapter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class ServerApp {

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplicationBuilder(ServerApp.class).build(args);
        springApplication.setWebApplicationType(WebApplicationType.REACTIVE);
        springApplication.run();
    }
}
