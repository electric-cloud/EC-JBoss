package com.cloudbees.pdk.hen.procedures

import groovy.transform.AutoClone
import com.cloudbees.pdk.hen.*
import com.cloudbees.pdk.hen.*

@AutoClone
//generated
class StartHostController extends Procedure {

    static StartHostController create(Plugin plugin) {
        return new StartHostController(procedureName: 'StartHostController', plugin: plugin, )
    }


    StartHostController flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    StartHostController withUser(User user) {
        this.contextUser = user
        return this
    }

    //Generated
    
    StartHostController additionalOptions(String additionalOptions) {
        this.addParam('additionalOptions', additionalOptions)
        return this
    }
    
    
    StartHostController domainConfig(String domainConfig) {
        this.addParam('domainConfig', domainConfig)
        return this
    }
    
    
    StartHostController hostConfig(String hostConfig) {
        this.addParam('hostConfig', hostConfig)
        return this
    }
    
    
    StartHostController jbossHostName(String jbossHostName) {
        this.addParam('jbossHostName', jbossHostName)
        return this
    }
    
    
    StartHostController logFileLocation(String logFileLocation) {
        this.addParam('logFileLocation', logFileLocation)
        return this
    }
    
    
    StartHostController serverconfig(String serverconfig) {
        this.addParam('serverconfig', serverconfig)
        return this
    }
    
    
    StartHostController startupScript(String startupScript) {
        this.addParam('startupScript', startupScript)
        return this
    }
    
    
    
    
}