package com.cloudbees.pdk.hen.procedures

import groovy.transform.AutoClone
import com.cloudbees.pdk.hen.*
import com.cloudbees.pdk.hen.*

@AutoClone
//generated
class StopServers extends Procedure {

    static StopServers create(Plugin plugin) {
        return new StopServers(procedureName: 'StopServers', plugin: plugin, )
    }


    StopServers flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    StopServers withUser(User user) {
        this.contextUser = user
        return this
    }

    //Generated
    
    StopServers scriptphysicalpath(String scriptphysicalpath) {
        this.addParam('scriptphysicalpath', scriptphysicalpath)
        return this
    }
    
    
    StopServers serverconfig(String serverconfig) {
        this.addParam('serverconfig', serverconfig)
        return this
    }
    
    
    StopServers serversgroup(String serversgroup) {
        this.addParam('serversgroup', serversgroup)
        return this
    }
    
    
    StopServers waittime(String waittime) {
        this.addParam('wait_time', waittime)
        return this
    }
    
    
    
    
}