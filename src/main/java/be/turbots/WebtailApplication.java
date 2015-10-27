package be.turbots;

import be.turbots.webtail.Server;
import be.turbots.webtail.ServerLog;
import be.turbots.webtail.ServerSettings;
import be.turbots.webtail.WebTailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@ComponentScan
@SpringBootApplication
@EnableAsync(proxyTargetClass = true)
public class WebtailApplication implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebtailApplication.class);

    @Autowired
    private WebTailer webTailer;

    @Autowired
    private ServerSettings serverSettings;

    public static void main(String[] args) {
        SpringApplication.run(WebtailApplication.class, args);
    }

    @Override
    public void run(String... strings) throws Exception {
        for (Server server : serverSettings.getServers()) {
            for (ServerLog serverLog : server.getServerLogs()) {
                webTailer.tailLog(server.getName(), serverLog);
            }
        }
    }

    @Bean(name = "myExecutor")
    public TaskExecutor workExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(20);
        taskExecutor.setMaxPoolSize(50);
        taskExecutor.setQueueCapacity(50);
        taskExecutor.afterPropertiesSet();
        return taskExecutor;
    }
}
