package com.syh.imhub;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.syh.imhub.modules.**.mapper")
public class ImHubServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImHubServerApplication.class, args);
    }

}
