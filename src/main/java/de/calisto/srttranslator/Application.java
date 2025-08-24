package de.calisto.srttranslator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication(scanBasePackages = "de.calisto.srttranslator")
@Configuration
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
