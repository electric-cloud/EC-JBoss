package com.cloudbees.pdk.hen.procedures

import groovy.transform.AutoClone
import com.cloudbees.pdk.hen.*
import com.cloudbees.pdk.hen.*

@AutoClone
//generated
class CheckHostControllerStatus extends Procedure {

    static CheckHostControllerStatus create(Plugin plugin) {
        return new CheckHostControllerStatus(procedureName: 'CheckHostControllerStatus', plugin: plugin, )
    }


    CheckHostControllerStatus flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    CheckHostControllerStatus withUser(User user) {
        this.contextUser = user
        return this
    }

    //Generated
    
    CheckHostControllerStatus criteria(String criteria) {
        this.addParam('criteria', criteria)
        return this
    }
    
    CheckHostControllerStatus criteria(CriteriaOptions criteria) {
        this.addParam('criteria', criteria.toString())
        return this
    }
    
    
    CheckHostControllerStatus hostcontrollername(String hostcontrollername) {
        this.addParam('hostcontroller_name', hostcontrollername)
        return this
    }
    
    
    CheckHostControllerStatus scriptphysicalpath(String scriptphysicalpath) {
        this.addParam('scriptphysicalpath', scriptphysicalpath)
        return this
    }
    
    
    CheckHostControllerStatus serverconfig(String serverconfig) {
        this.addParam('serverconfig', serverconfig)
        return this
    }
    
    
    CheckHostControllerStatus waittime(String waittime) {
        this.addParam('wait_time', waittime)
        return this
    }
    
    
    
    
    enum CriteriaOptions {
    
    RUNNING("RUNNING"),
    
    NOT_RUNNING("NOT_RUNNING")
    
    private String value
    CriteriaOptions(String value) {
        this.value = value
    }

    String toString() {
        return this.value
    }
}
    
}