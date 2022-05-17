package com.cloudbees.pdk.hen.procedures

import groovy.transform.AutoClone
import com.cloudbees.pdk.hen.*
import com.cloudbees.pdk.hen.*

@AutoClone
//generated
class DisableDeploy extends Procedure {

    static DisableDeploy create(Plugin plugin) {
        return new DisableDeploy(procedureName: 'DisableDeploy', plugin: plugin, )
    }


    DisableDeploy flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    DisableDeploy withUser(User user) {
        this.contextUser = user
        return this
    }

    //Generated
    
    DisableDeploy appname(String appname) {
        this.addParam('appname', appname)
        return this
    }
    
    
    DisableDeploy assignservergroups(String assignservergroups) {
        this.addParam('assignservergroups', assignservergroups)
        return this
    }
    
    
    DisableDeploy scriptphysicalpath(String scriptphysicalpath) {
        this.addParam('scriptphysicalpath', scriptphysicalpath)
        return this
    }
    
    
    DisableDeploy serverconfig(String serverconfig) {
        this.addParam('serverconfig', serverconfig)
        return this
    }
    
    
    
    
}