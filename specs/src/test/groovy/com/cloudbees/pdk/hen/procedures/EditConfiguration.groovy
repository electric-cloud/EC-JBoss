package com.cloudbees.pdk.hen.procedures

import groovy.transform.AutoClone
import com.cloudbees.pdk.hen.*
import com.cloudbees.pdk.hen.*

@AutoClone
//generated
class EditConfiguration extends Procedure {

    static EditConfiguration create(Plugin plugin) {
        return new EditConfiguration(procedureName: 'EditConfiguration', plugin: plugin, credentials: [
            
            'credential': null,
            
        ])
    }


    EditConfiguration flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    EditConfiguration withUser(User user) {
        this.contextUser = user
        return this
    }

    //Generated
    
    EditConfiguration checkConnection(boolean checkConnection) {
        this.addParam('checkConnection', checkConnection)
        return this
    }
    
    
    EditConfiguration config(String config) {
        this.addParam('config', config)
        return this
    }
    
    
    EditConfiguration desc(String desc) {
        this.addParam('desc', desc)
        return this
    }
    
    
    EditConfiguration javaopts(String javaopts) {
        this.addParam('java_opts', javaopts)
        return this
    }
    
    
    EditConfiguration jbossurl(String jbossurl) {
        this.addParam('jboss_url', jbossurl)
        return this
    }
    
    
    EditConfiguration loglevel(String loglevel) {
        this.addParam('log_level', loglevel)
        return this
    }
    
    EditConfiguration loglevel(Log_levelOptions loglevel) {
        this.addParam('log_level', loglevel.toString())
        return this
    }
    
    
    EditConfiguration scriptphysicalpath(String scriptphysicalpath) {
        this.addParam('scriptphysicalpath', scriptphysicalpath)
        return this
    }
    
    
    EditConfiguration testconnectionres(String testconnectionres) {
        this.addParam('test_connection_res', testconnectionres)
        return this
    }
    
    
    
    EditConfiguration credential(String user, String password) {
        this.addCredential('credential', user, password)
        return this
    }

    EditConfiguration credentialReference(String path) {
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