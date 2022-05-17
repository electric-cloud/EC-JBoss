package com.cloudbees.pdk.hen

import com.cloudbees.pdk.hen.procedures.*
import com.cloudbees.pdk.hen.Plugin

import static com.cloudbees.pdk.hen.Utils.env

class Rest extends Plugin {

    static Rest create() {
        Rest plugin = new Rest(name: 'EC-Rest')
        plugin.configure(plugin.config)
        return plugin
    }
    static Rest createWithoutConfig() {
        Rest plugin = new Rest(name: 'EC-Rest')
        return plugin
    }

    //user-defined after boilerplate was generated, default parameters setup
    RestConfig config = RestConfig
        .create(this)
        //.parameter(value) add parameters here


    EditConfiguration editConfiguration = EditConfiguration.create(this)

    RunRest runRest = RunRest.create(this)

    TestConfiguration testConfiguration = TestConfiguration.create(this)

}