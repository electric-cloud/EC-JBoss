package com.cloudbees.pdk.hen.procedures

import groovy.transform.AutoClone
import com.cloudbees.pdk.hen.*
import com.cloudbees.pdk.hen.*

@AutoClone
//generated
class ShutdownStandaloneServer extends Procedure {

    static ShutdownStandaloneServer create(Plugin plugin) {
        return new ShutdownStandaloneServer(procedureName: 'ShutdownStandaloneServer', plugin: plugin, )
    }


    ShutdownStandaloneServer flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    ShutdownStandaloneServer withUser(User user) {
        this.contextUser = user
        return this
    }

    //Generated
    
    ShutdownStandaloneServer scriptphysicalpath(String scriptphysicalpath) {
        this.addParam('scriptphysicalpath', scriptphysicalpath)
        return this
    }
    
    
    ShutdownStandaloneServer serverconfig(String serverconfig) {
        this.addParam('serverconfig', serverconfig)
        return this
    }
    
    
    
    
}