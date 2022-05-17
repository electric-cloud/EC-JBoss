package com.cloudbees.pdk.hen.procedures

import groovy.transform.AutoClone
import com.cloudbees.pdk.hen.*
import com.cloudbees.pdk.hen.*

@AutoClone
//generated
class StartServers extends Procedure {

    static StartServers create(Plugin plugin) {
        return new StartServers(procedureName: 'StartServers', plugin: plugin, )
    }


    StartServers flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    StartServers withUser(User user) {
        this.contextUser = user
        return this
    }

    //Generated
    
    StartServers scriptphysicalpath(String scriptphysicalpath) {
        this.addParam('scriptphysicalpath', scriptphysicalpath)
        return this
    }
    
    
    StartServers serverconfig(String serverconfig) {
        this.addParam('serverconfig', serverconfig)
        return this
    }
    
    
    StartServers serversgroup(String serversgroup) {
        this.addParam('serversgroup', serversgroup)
        return this
    }
    
    
    StartServers waittime(String waittime) {
        this.addParam('wait_time', waittime)
        return this
    }
    
    
    
    
}