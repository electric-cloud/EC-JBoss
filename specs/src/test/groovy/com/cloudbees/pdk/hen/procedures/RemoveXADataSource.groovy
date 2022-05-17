package com.cloudbees.pdk.hen.procedures

import groovy.transform.AutoClone
import com.cloudbees.pdk.hen.*
import com.cloudbees.pdk.hen.*

@AutoClone
//generated
class RemoveXADataSource extends Procedure {

    static RemoveXADataSource create(Plugin plugin) {
        return new RemoveXADataSource(procedureName: 'RemoveXADataSource', plugin: plugin, )
    }


    RemoveXADataSource flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    RemoveXADataSource withUser(User user) {
        this.contextUser = user
        return this
    }

    //Generated
    
    RemoveXADataSource dataSourceName(String dataSourceName) {
        this.addParam('dataSourceName', dataSourceName)
        return this
    }
    
    
    RemoveXADataSource profile(String profile) {
        this.addParam('profile', profile)
        return this
    }
    
    
    RemoveXADataSource serverconfig(String serverconfig) {
        this.addParam('serverconfig', serverconfig)
        return this
    }
    
    
    
    
}