package com.electriccloud.plugin.spec.Utils

class EnvPropertiesHelper {
    public static String getJbossUsername() {
        return System.getenv('JBOSS_USERNAME') ?: 'admin'
    }

    public static String getJbossPassword() {
        return System.getenv('JBOSS_PASSWORD') ?: 'changeme'
    }

    public static String getJbossControllerUrl() {
        return System.getenv('JBOSS_CONTROLLER_URL') ?: 'jboss:9990'
    }

    public static String getJbossCliPath() {
        return System.getenv('JBOSS_CLI_PATH') ?: '/opt/jboss/bin/jboss-cli.sh'
    }

    public static String getJbossDomainPath() {
        return System.getenv('JBOSS_DOMAIN_PATH') ?: '/opt/jboss/bin/domain.sh'
    }

    public static String getJbossLogLevel() {
        return System.getenv('JBOSS_LOG_LEVEL') ?: 'DEBUG'
    }

    public static String getJbossLogLevelValue() {
        switch (getJbossLogLevel()) {
            case "INFO": return 1;
            case "WARNING": return 2;
            case "ERROR": return 3;
            case "DEBUG": return 4;
        }
    }

    public static String getJbossDomainMasterHostname() {
        return System.getenv('JBOSS_DOMAIN_MASTER_HOSTNAME') ?: 'master'
    }

    public static String getJbossDomainSlaveHostname() {
        return System.getenv('JBOSS_DOMAIN_SLAVE_HOSTNAME') ?: 'slave'
    }

    public static String getResourceHostname() {
        return System.getenv('RESOURCE_HOSTNAME') ?: 'jboss'
    }

    public static String getResourceSlaveHostname() {
        return System.getenv('RESOURCE_SLAVE1_HOSTNAME') ?: 'jboss-slave1'
    }

    public static String getResourcePort() {
        return System.getenv('RESOURCE_PORT') ?: 7808;
    }

    public static String getResourcePortSlave() {
        return System.getenv('RESOURCE_PORT_SLAVE1') ?: 7808;
    }

    public static String getServerConfigStandalone() {
        return System.getenv('SERVER_CONFIG') ?: 'standalone-full.xml';
    }

    public static String getOS() {
        return System.getenv('OS') ?: 'UNIX'
    }

    public static String isUnix() {
        return getOS() == "UNIX"
    }

    public static String isWindows() {
        return getOS() == "WINDOWS"
    }

    public static String getVersion() {
        return System.getenv('JBOSS_VERSION')
    }

    public static String getMode() {
        return System.getenv('JBOSS_MODE')
    }

    public static String getTopology() {
        return System.getenv('JBOSS_TOPOLOGY')
    }
}