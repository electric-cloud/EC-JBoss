package com.cloudbees.pdk.hen.procedures

import groovy.transform.AutoClone
import com.cloudbees.pdk.hen.*
import com.cloudbees.pdk.hen.*

@AutoClone
//generated
class UndeployApp extends Procedure {

    static UndeployApp create(Plugin plugin) {
        return new UndeployApp(procedureName: 'UndeployApp', plugin: plugin, )
    }


    UndeployApp flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    UndeployApp withUser(User user) {
        this.contextUser = user
        return this
    }

    //Generated
    
    UndeployApp additionaloptions(String additionaloptions) {
        this.addParam('additional_options', additionaloptions)
        return this
    }
    
    
    UndeployApp allrelevantservergroups(boolean allrelevantservergroups) {
        this.addParam('allrelevantservergroups', allrelevantservergroups)
        return this
    }
    
    
    UndeployApp appname(String appname) {
        this.addParam('appname', appname)
        return this
    }
    
    
    UndeployApp keepcontent(boolean keepcontent) {
        this.addParam('keepcontent', keepcontent)
        return this
    }
    
    
    UndeployApp scriptphysicalpath(String scriptphysicalpath) {
        this.addParam('scriptphysicalpath', scriptphysicalpath)
        return this
    }
    
    
    UndeployApp serverconfig(String serverconfig) {
        this.addParam('serverconfig', serverconfig)
        return this
    }
    
    
    UndeployApp servergroups(String servergroups) {
        this.addParam('servergroups', servergroups)
        return this
    }
    
    
    
    
}