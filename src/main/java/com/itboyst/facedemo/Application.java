package com.itboyst.facedemo;

import com.itboyst.facedemo.camera.CameraUtil;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@MapperScan("com.itboyst.facedemo.dao.mapper")
@EnableTransactionManagement
public class Application {


    public static void main(String[] args) {
        System.out.println("java.library.path=" + System.getProperty("java.library.path"));
        SpringApplication.run(Application.class, args);
    }


}

