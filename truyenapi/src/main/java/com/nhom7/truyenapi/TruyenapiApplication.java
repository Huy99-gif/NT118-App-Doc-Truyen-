package com.nhom7.truyenapi;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TruyenapiApplication {

	static {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
  }
	public static void main(String[] args) {
		SpringApplication.run(TruyenapiApplication.class, args);
	}

}
