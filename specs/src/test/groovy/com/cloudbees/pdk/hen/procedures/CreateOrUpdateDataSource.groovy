package com.cloudbees.pdk.hen.procedures

import groovy.transform.AutoClone
import com.cloudbees.pdk.hen.*
import com.cloudbees.pdk.hen.*

@AutoClone
//generated
class CreateOrUpdateDataSource extends Procedure {

    static CreateOrUpdateDataSource create(Plugin plugin) {
        return new CreateOrUpdateDataSource(procedureName: 'CreateOrUpdateDataSource', plugin: plugin, credentials: [
            
            'dataSourceConnection_credential': null,
            
        ])
    }


    CreateOrUpdateDataSource flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    CreateOrUpdateDataSource withUser(User user) {
        this.contextUser = user
        return this
    }

    //Generated
    
    CreateOrUpdateDataSource additionalOptions(String additionalOptions) {
        this.addParam('additionalOptions', additionalOptions)
        return this
    }
    
    
    CreateOrUpdateDataSource connectionUrl(String connectionUrl) {
        this.addParam('connectionUrl', connectionUrl)
        return this
    }
    
    
    CreateOrUpdateDataSource dataSourceName(String dataSourceName) {
        this.addParam('dataSourceName', dataSourceName)
        return this
    }
    
    
    CreateOrUpdateDataSource enabled(String enabled) {
        this.addParam('enabled', enabled)
        return this
    }
    
    
    CreateOrUpdateDataSource jdbcDriverName(String jdbcDriverName) {
        this.addParam('jdbcDriverName', jdbcDriverName)
        return this
    }
    
    
    CreateOrUpdateDataSource jndiName(String jndiName) {
        this.addParam('jndiName', jndiName)
        return this
    }
    
    
    CreateOrUpdateDataSource profile(String profile) {
        this.addParam('profile', profile)
        return this
    }
    
    
    CreateOrUpdateDataSource serverconfig(String serverconfig) {
        this.addParam('serverconfig', serverconfig)
        return this
    }
    
    
    
    CreateOrUpdateDataSource dataSourceConnection_credential(String user, String password) {
        this.addCredential('dataSourceConnection_credential', user, password)
        return this
    }

    CreateOrUpdateDataSource dataSourceConnection_credentialReference(String path) {
        this.addCredentialReference('dataSourceConnection_credential', path)
        return this
    }
    
    
}