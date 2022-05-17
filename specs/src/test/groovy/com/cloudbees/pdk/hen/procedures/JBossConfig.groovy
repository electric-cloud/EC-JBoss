package com.cloudbees.pdk.hen.procedures

import groovy.transform.AutoClone
import com.cloudbees.pdk.hen.*
import com.cloudbees.pdk.hen.*

@AutoClone
//generated
class JBossConfig extends Procedure {

    static JBossConfig create(Plugin plugin) {
        return new JBossConfig(procedureName: 'CreateConfiguration', plugin: plugin, credentials: [
            
            'credential': null,
            
        ])
    }


    JBossConfig flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    JBossConfig withUser(User user) {
        this.contextUser = user
        return this
    }

    //Generated
    
    JBossConfig checkConnection(boolean checkConnection) {
        this.addParam('checkConnection', checkConnection)
        return this
    }
    
    
    JBossConfig config(String config) {
        this.addParam('config', config)
        return this
    }
    
    
    JBossConfig desc(String desc) {
        this.addParam('desc', desc)
        return this
    }
    
    
    JBossConfig javaopts(String javaopts) {
        this.addParam('java_opts', javaopts)
        return this
    }
    
    
    JBossConfig jbossurl(String jbossurl) {
        this.addParam('jboss_url', jbossurl)
        return this
    }
    
    
    JBossConfig loglevel(String loglevel) {
        this.addParam('log_level', loglevel)
        return this
    }
    
    JBossConfig loglevel(Log_levelOptions loglevel) {
        this.addParam('log_level', loglevel.toString())
        return this
    }
    
    
    JBossConfig scriptphysicalpath(String scriptphysicalpath) {
        this.addParam('scriptphysicalpath', scriptphysicalpath)
        return this
    }
    
    
    JBossConfig testconnectionres(String testconnectionres) {
        this.addParam('test_connection_res', testconnectionres)
        return this
    }
    
    
    
    JBossConfig credential(String user, String password) {
        this.addCredential('credential', user, password)
        return this
    }

    JBossConfig credentialReference(String path) {
        this.addCredentialReference('credential', path)
        return this
    }
    
    
    enum Log_levelOptions {
    
    INFO("1"),
    
    WARNING("2"),
    
    ERROR("3"),
    
    DEBUG("4")
    
    private String value
    Log_levelOptions(String value) {
        this.value = value
    }

    String toString() {
        return this.value
    }
}
    
}