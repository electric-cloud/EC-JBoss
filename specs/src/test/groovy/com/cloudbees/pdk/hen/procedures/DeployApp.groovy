package com.cloudbees.pdk.hen.procedures

import groovy.transform.AutoClone
import com.cloudbees.pdk.hen.*
import com.cloudbees.pdk.hen.*

@AutoClone
//generated
class DeployApp extends Procedure {

    static DeployApp create(Plugin plugin) {
        return new DeployApp(procedureName: 'DeployApp', plugin: plugin, )
    }


    DeployApp flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    DeployApp withUser(User user) {
        this.contextUser = user
        return this
    }

    //Generated
    
    DeployApp additionaloptions(String additionaloptions) {
        this.addParam('additional_options', additionaloptions)
        return this
    }
    
    
    DeployApp appname(String appname) {
        this.addParam('appname', appname)
        return this
    }
    
    
    DeployApp assignallservergroups(boolean assignallservergroups) {
        this.addParam('assignallservergroups', assignallservergroups)
        return this
    }
    
    
    DeployApp assignservergroups(String assignservergroups) {
        this.addParam('assignservergroups', assignservergroups)
        return this
    }
    
    
    DeployApp force(boolean force) {
        this.addParam('force', force)
        return this
    }
    
    
    DeployApp runtimename(String runtimename) {
        this.addParam('runtimename', runtimename)
        return this
    }
    
    
    DeployApp scriptphysicalpath(String scriptphysicalpath) {
        this.addParam('scriptphysicalpath', scriptphysicalpath)
        return this
    }
    
    
    DeployApp serverconfig(String serverconfig) {
        this.addParam('serverconfig', serverconfig)
        return this
    }
    
    
    DeployApp warphysicalpath(String warphysicalpath) {
        this.addParam('warphysicalpath', warphysicalpath)
        return this
    }
    
    
    
    
}