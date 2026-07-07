package ao.skool.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "ao.skool")
public class SkoolApplication {

    public static void main(String[] args) {
        SpringApplication.run(SkoolApplication.class, args);
    }
}
