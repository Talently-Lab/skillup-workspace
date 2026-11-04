package skillup_workspace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SkillupWorkspaceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SkillupWorkspaceApplication.class, args);
	}

}
