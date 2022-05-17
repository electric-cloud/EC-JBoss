package com.cloudbees.pdk.hen.procedures

import groovy.transform.AutoClone
import com.cloudbees.pdk.hen.*
import com.cloudbees.pdk.hen.*

@AutoClone
//generated
class TestConfiguration extends Procedure {

    static TestConfiguration create(Plugin plugin) {
        return new TestConfiguration(procedureName: 'TestConfiguration', plugin: plugin, credentials: [
            
            'credential': null,
            
        ])
    }


    TestConfiguration flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    TestConfiguration withUser(User user) {
        this.contextUser = user
        return this
    }

    //Generated
    
    TestConfiguration checkConnection(boolean checkConnection) {
        this.addParam('checkConnection', checkConnection)
        return this
    }
    
    
    TestConfiguration config(String config) {
        this.addParam('config', config)
        return this
    }
    
    
    TestConfiguration desc(String desc) {
        this.addParam('desc', desc)
        return this
    }
    
    
    TestConfiguration javaopts(String javaopts) {
        this.addParam('java_opts', javaopts)
        return this
    }
    
    
    TestConfiguration jbossurl(String jbossurl) {
        this.addParam('jboss_url', jbossurl)
        return this
    }
    
    
    TestConfiguration loglevel(String loglevel) {
        this.addParam('log_level', loglevel)
        return this
    }
    
    TestConfiguration loglevel(Log_levelOptions loglevel) {
        this.addParam('log_level', loglevel.toString())
        return this
    }
    
    
    TestConfiguration scriptphysicalpath(String scriptphysicalpath) {
        this.addParam('scriptphysicalpath', scriptphysicalpath)
        return this
    }
    
    
    TestConfiguration testconnectionres(String testconnectionres) {
        this.addParam('test_connection_res', testconnectionres)
        return this
    }
    
    
    
    TestConfiguration credential(String user, String password) {
        this.addCredential('credential', user, password)
        return this
    }

    TestConfiguration credentialReference(String path) {
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