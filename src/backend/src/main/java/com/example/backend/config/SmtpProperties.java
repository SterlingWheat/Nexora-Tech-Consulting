package com.example.backend.config;

public class SmtpProperties {

    private String host = "";
    private int port = 465;
    private boolean secure = true;
    private String username = "";
    private String password = "";
    private String from = "";

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public boolean isConfigured() {
        return hasText(host)
                && port > 0
                && port <= 65535
                && hasText(username)
                && hasText(password)
                && hasText(from);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
