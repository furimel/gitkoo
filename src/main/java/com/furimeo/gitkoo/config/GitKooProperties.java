package com.furimeo.gitkoo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Top-level GitKoo configuration bound from {@code gitkoo.*} in application.yaml.
 *
 * @see DESIGN.md §37, §43
 */
@ConfigurationProperties(prefix = "gitkoo")
public class GitKooProperties {

    /** Root data directory. All GitKoo storage lives under here. */
    private String data = "./data";

    private Git git = new Git();
    private Ssh ssh = new Ssh();
    private Ci ci = new Ci();

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Git getGit() {
        return git;
    }

    public void setGit(Git git) {
        this.git = git;
    }

    public Ssh getSsh() {
        return ssh;
    }

    public void setSsh(Ssh ssh) {
        this.ssh = ssh;
    }

    public Ci getCi() {
        return ci;
    }

    public void setCi(Ci ci) {
        this.ci = ci;
    }

    public static class Git {
        /** Git executable name or path used to invoke the Git CLI. */
        private String binary = "git";

        public String getBinary() {
            return binary;
        }

        public void setBinary(String binary) {
            this.binary = binary;
        }
    }

    public static class Ssh {
        private boolean enabled = true;
        private int port = 2222;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    public static class Ci {
        private boolean enabled = true;
        private int workers = 2;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getWorkers() {
            return workers;
        }

        public void setWorkers(int workers) {
            this.workers = workers;
        }
    }
}
