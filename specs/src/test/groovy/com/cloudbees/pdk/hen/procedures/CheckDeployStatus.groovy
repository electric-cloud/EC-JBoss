package com.cloudbees.pdk.hen.procedures

import groovy.transform.AutoClone
import com.cloudbees.pdk.hen.*
import com.cloudbees.pdk.hen.*

@AutoClone
//generated
class CheckDeployStatus extends Procedure {

    static CheckDeployStatus create(Plugin plugin) {
        return new CheckDeployStatus(procedureName: 'CheckDeployStatus', plugin: plugin, )
    }


    CheckDeployStatus flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    CheckDeployStatus withUser(User user) {
        this.contextUser = user
        return this
    }

    //Generated
    
    CheckDeployStatus appname(String appname) {
        this.addParam('appname', appname)
        return this
    }
    
    
    CheckDeployStatus criteria(String criteria) {
        this.addParam('criteria', criteria)
        return this
    }
    
    CheckDeployStatus criteria(CriteriaOptions criteria) {
        this.addParam('criteria', criteria.toString())
        return this
    }
    
    
    CheckDeployStatus hosts(String hosts) {
        this.addParam('hosts', hosts)
        return this
    }
    
    
    CheckDeployStatus scriptphysicalpath(String scriptphysicalpath) {
        this.addParam('scriptphysicalpath', scriptphysicalpath)
        return this
    }
    
    
    CheckDeployStatus serverconfig(String serverconfig) {
        this.addParam('serverconfig', serverconfig)
        return this
    }
    
    
    CheckDeployStatus servers(String servers) {
        this.addParam('servers', servers)
        return this
    }
    
    
    CheckDeployStatus serversgroup(String serversgroup) {
        this.addParam('serversgroup', serversgroup)
        return this
    }
    
    
    CheckDeployStatus waittime(String waittime) {
        this.addParam('wait_time', waittime)
        return this
    }
    
    
    
    
    enum CriteriaOptions {
    
    OK("OK"),
    
    NOT_OK("NOT_OK")
    
    private String value
    CriteriaOptions(String value) {
        this.value = value
    }

    String toString() {
        return this.value
    }
}
    
}