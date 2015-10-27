package be.turbots.webtail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties
public class ServerSettings {

    private Server[] servers;

    public Server[] getServers() {
        return servers;
    }

    @SuppressWarnings("unused")
    public void setServers(Server[] servers) {
        this.servers = servers;
    }
}
