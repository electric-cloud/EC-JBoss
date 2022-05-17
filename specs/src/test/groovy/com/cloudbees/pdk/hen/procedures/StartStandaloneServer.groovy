package com.cloudbees.pdk.hen.procedures

import groovy.transform.AutoClone
import com.cloudbees.pdk.hen.*
import com.cloudbees.pdk.hen.*

@AutoClone
//generated
class StartStandaloneServer extends Procedure {

    static StartStandaloneServer create(Plugin plugin) {
        return new StartStandaloneServer(procedureName: 'StartStandaloneServer', plugin: plugin, )
    }


    StartStandaloneServer flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    StartStandaloneServer withUser(User user) {
        this.contextUser = user
        return this
    }

    //Generated
    
    StartStandaloneServer additionalOptions(String additionalOptions) {
        this.addParam('additionalOptions', additionalOptions)
        return this
    }
    
    
    StartStandaloneServer alternatejbossconfig(String alternatejbossconfig) {
        this.addParam('alternatejbossconfig', alternatejbossconfig)
        return this
    }
    
    
    StartStandaloneServer logFileLocation(String logFileLocation) {
        this.addParam('logFileLocation', logFileLocation)
        return this
    }
    
    
    StartStandaloneServer scriptphysicalpath(String scriptphysicalpath) {
        this.addParam('scriptphysicalpath', scriptphysicalpath)
        return this
    }
    
    
    StartStandaloneServer serverconfig(String serverconfig) {
        this.addParam('serverconfig', serverconfig)
        return this
    }
    
    
    
    
}