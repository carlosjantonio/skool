package ao.skool.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "ao.skool")
@EntityScan(basePackages = "ao.skool")
@EnableJpaRepositories(basePackages = "ao.skool")
public class SkoolApplication {

    public static void main(String[] args) {
        SpringApplication.run(SkoolApplication.class, args);
    }
}
