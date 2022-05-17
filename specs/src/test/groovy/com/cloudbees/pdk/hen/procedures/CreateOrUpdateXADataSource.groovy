package com.cloudbees.pdk.hen.procedures

import groovy.transform.AutoClone
import com.cloudbees.pdk.hen.*
import com.cloudbees.pdk.hen.*

@AutoClone
//generated
class CreateOrUpdateXADataSource extends Procedure {

    static CreateOrUpdateXADataSource create(Plugin plugin) {
        return new CreateOrUpdateXADataSource(procedureName: 'CreateOrUpdateXADataSource', plugin: plugin, credentials: [
            
            'dataSourceConnection_credential': null,
            
        ])
    }


    CreateOrUpdateXADataSource flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    CreateOrUpdateXADataSource withUser(User user) {
        this.contextUser = user
        return this
    }

    //Generated
    
    CreateOrUpdateXADataSource additionalOptions(String additionalOptions) {
        this.addParam('additionalOptions', additionalOptions)
        return this
    }
    
    
    CreateOrUpdateXADataSource dataSourceName(String dataSourceName) {
        this.addParam('dataSourceName', dataSourceName)
        return this
    }
    
    
    CreateOrUpdateXADataSource enabled(String enabled) {
        this.addParam('enabled', enabled)
        return this
    }
    
    
    CreateOrUpdateXADataSource jdbcDriverName(String jdbcDriverName) {
        this.addParam('jdbcDriverName', jdbcDriverName)
        return this
    }
    
    
    CreateOrUpdateXADataSource jndiName(String jndiName) {
        this.addParam('jndiName', jndiName)
        return this
    }
    
    
    CreateOrUpdateXADataSource profile(String profile) {
        this.addParam('profile', profile)
        return this
    }
    
    
    CreateOrUpdateXADataSource serverconfig(String serverconfig) {
        this.addParam('serverconfig', serverconfig)
        return this
    }
    
    
    CreateOrUpdateXADataSource xaDataSourceProperties(String xaDataSourceProperties) {
        this.addParam('xaDataSourceProperties', xaDataSourceProperties)
        return this
    }
    
    
    
    CreateOrUpdateXADataSource dataSourceConnection_credential(String user, String password) {
        this.addCredential('dataSourceConnection_credential', user, password)
        return this
    }

    CreateOrUpdateXADataSource dataSourceConnection_credentialReference(String path) {
        this.addCredentialReference('dataSourceConnection_credential', path)
        return this
    }
    
    
}