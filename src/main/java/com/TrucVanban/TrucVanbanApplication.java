package com.TrucVanban;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class TrucVanbanApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

		Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
		for (DotenvEntry entry : dotenv.entries()) {
			System.setProperty(entry.getKey(), entry.getValue());
		}
		SpringApplication.run(TrucVanbanApplication.class, args);
	}

}
