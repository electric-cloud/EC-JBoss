package com.cloudbees.pdk.hen.procedures

import groovy.transform.AutoClone
import com.cloudbees.pdk.hen.*
import com.cloudbees.pdk.hen.*

@AutoClone
//generated
class CreateOrUpdateJMSQueue extends Procedure {

    static CreateOrUpdateJMSQueue create(Plugin plugin) {
        return new CreateOrUpdateJMSQueue(procedureName: 'CreateOrUpdateJMSQueue', plugin: plugin, )
    }


    CreateOrUpdateJMSQueue flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    CreateOrUpdateJMSQueue withUser(User user) {
        this.contextUser = user
        return this
    }

    //Generated
    
    CreateOrUpdateJMSQueue additionalOptions(String additionalOptions) {
        this.addParam('additionalOptions', additionalOptions)
        return this
    }
    
    
    CreateOrUpdateJMSQueue durable(String durable) {
        this.addParam('durable', durable)
        return this
    }
    
    
    CreateOrUpdateJMSQueue jndiNames(String jndiNames) {
        this.addParam('jndiNames', jndiNames)
        return this
    }
    
    
    CreateOrUpdateJMSQueue messageSelector(String messageSelector) {
        this.addParam('messageSelector', messageSelector)
        return this
    }
    
    
    CreateOrUpdateJMSQueue profile(String profile) {
        this.addParam('profile', profile)
        return this
    }
    
    
    CreateOrUpdateJMSQueue queueName(String queueName) {
        this.addParam('queueName', queueName)
        return this
    }
    
    
    CreateOrUpdateJMSQueue serverconfig(String serverconfig) {
        this.addParam('serverconfig', serverconfig)
        return this
    }
    
    
    
    
}