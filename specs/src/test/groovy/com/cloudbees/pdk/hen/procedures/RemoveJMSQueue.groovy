package com.cloudbees.pdk.hen.procedures

import groovy.transform.AutoClone
import com.cloudbees.pdk.hen.*
import com.cloudbees.pdk.hen.*

@AutoClone
//generated
class RemoveJMSQueue extends Procedure {

    static RemoveJMSQueue create(Plugin plugin) {
        return new RemoveJMSQueue(procedureName: 'RemoveJMSQueue', plugin: plugin, )
    }


    RemoveJMSQueue flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    RemoveJMSQueue withUser(User user) {
        this.contextUser = user
        return this
    }

    //Generated
    
    RemoveJMSQueue profile(String profile) {
        this.addParam('profile', profile)
        return this
    }
    
    
    RemoveJMSQueue queueName(String queueName) {
        this.addParam('queueName', queueName)
        return this
    }
    
    
    RemoveJMSQueue serverconfig(String serverconfig) {
        this.addParam('serverconfig', serverconfig)
        return this
    }
    
    
    
    
}