package com.electriccloud.plugin.spec.Utils

class EnvPropertiesHelper {
    static String getJbossUsername() {
        return System.getenv('JBOSS_USERNAME') ?: 'admin'
    }

    static String getJbossPassword() {
        return System.getenv('JBOSS_PASSWORD') ?: 'changeme'
    }

    static String getJbossControllerUrl() {
        return System.getenv('JBOSS_CONTROLLER_URL') ?: 'jboss:9990'
    }

    static String getJbossCliPath() {
        return System.getenv('JBOSS_CLI_PATH') ?: '/opt/jboss/bin/jboss-cli.sh'
    }

    static String getJbossDomainPath() {
        return System.getenv('JBOSS_DOMAIN_PATH') ?: '/opt/jboss/bin/domain.sh'
    }

    static String getJbossLogLevel() {
        return System.getenv('JBOSS_LOG_LEVEL') ?: 'DEBUG'
    }

    static String getJbossLogLevelValue() {
        switch (getJbossLogLevel()) {
            case "INFO": return 1
            case "WARNING": return 2
            case "ERROR": return 3
            case "DEBUG": return 4
        }
    }

    static String getJbossDomainMasterHostname() {
        return System.getenv('JBOSS_DOMAIN_MASTER_HOSTNAME') ?: 'master'
    }

    static String getJbossDomainSlaveHostname() {
        return System.getenv('JBOSS_DOMAIN_SLAVE_HOSTNAME') ?: 'slave'
    }

    static String getResourceHostname() {
        return System.getenv('RESOURCE_HOSTNAME') ?: 'jboss'
    }

    static String getResourceSlaveHostname() {
        return System.getenv('RESOURCE_SLAVE1_HOSTNAME') ?: 'jboss-slave1'
    }

    static String getResourcePort() {
        return System.getenv('RESOURCE_PORT') ?: 7808
    }

    static String getResourcePortSlave() {
        return System.getenv('RESOURCE_PORT_SLAVE1') ?: 7808
    }

    static String getServerConfigStandalone() {
        return System.getenv('SERVER_CONFIG') ?: 'standalone-full.xml'
    }

    static String getOS() {
        return System.getenv('OS') ?: 'UNIX'
    }

    static String isUnix() {
        return getOS() == "UNIX"
    }

    static String isWindows() {
        return getOS() == "WINDOWS"
    }

    static String getVersion() {
        return System.getenv('JBOSS_VERSION')
    }

    static String getMode() {
        return System.getenv('JBOSS_MODE')
    }

    static String getTopology() {
        return System.getenv('JBOSS_TOPOLOGY')
    }
}