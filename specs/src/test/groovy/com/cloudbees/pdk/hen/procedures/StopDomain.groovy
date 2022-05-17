package com.cloudbees.pdk.hen.procedures

import groovy.transform.AutoClone
import com.cloudbees.pdk.hen.*
import com.cloudbees.pdk.hen.*

@AutoClone
//generated
class StopDomain extends Procedure {

    static StopDomain create(Plugin plugin) {
        return new StopDomain(procedureName: 'StopDomain', plugin: plugin, )
    }


    StopDomain flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    StopDomain withUser(User user) {
        this.contextUser = user
        return this
    }

    //Generated
    
    StopDomain allControllersShutdown(String allControllersShutdown) {
        this.addParam('allControllersShutdown', allControllersShutdown)
        return this
    }
    
    
    StopDomain jbossTimeout(String jbossTimeout) {
        this.addParam('jbossTimeout', jbossTimeout)
        return this
    }
    
    
    StopDomain serverconfig(String serverconfig) {
        this.addParam('serverconfig', serverconfig)
        return this
    }
    
    
    
    
}