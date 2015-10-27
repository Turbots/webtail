package be.turbots.webtail;

public class Server {

    private String name;
    private ServerLog[] serverLogs;

    public String getName() {
        return name;
    }

    @SuppressWarnings("unused")
    public void setName(String name) {
        this.name = name;
    }

    public ServerLog[] getServerLogs() {
        return serverLogs;
    }

    @SuppressWarnings("unused")
    public void setServerLogs(ServerLog[] serverLogs) {
        this.serverLogs = serverLogs;
    }
}
