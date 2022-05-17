package com.cloudbees.pdk.hen.procedures

import groovy.transform.AutoClone
import com.cloudbees.pdk.hen.*
import com.cloudbees.pdk.hen.*

@AutoClone
//generated
class EnableDeploy extends Procedure {

    static EnableDeploy create(Plugin plugin) {
        return new EnableDeploy(procedureName: 'EnableDeploy', plugin: plugin, )
    }


    EnableDeploy flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    EnableDeploy withUser(User user) {
        this.contextUser = user
        return this
    }

    //Generated
    
    EnableDeploy appname(String appname) {
        this.addParam('appname', appname)
        return this
    }
    
    
    EnableDeploy assignservergroups(String assignservergroups) {
        this.addParam('assignservergroups', assignservergroups)
        return this
    }
    
    
    EnableDeploy scriptphysicalpath(String scriptphysicalpath) {
        this.addParam('scriptphysicalpath', scriptphysicalpath)
        return this
    }
    
    
    EnableDeploy serverconfig(String serverconfig) {
        this.addParam('serverconfig', serverconfig)
        return this
    }
    
    
    
    
}