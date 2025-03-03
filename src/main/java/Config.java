public class Config {
    public Host host;
    public Endpoint health_status;
    public Endpoint login;
    public Endpoint register;
    public Endpoint user;

    public static class Host {
        public String url;
        public Integer port;
    }

    public static class Endpoint {
        public Host host;
        public String path;
    }
}
