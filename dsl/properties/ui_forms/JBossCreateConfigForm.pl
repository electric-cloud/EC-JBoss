<editor>
    <formElement>
        <type>entry</type>
        <label>Configuration name:</label>
        <property>config</property>
        <value></value>
        <required>1</required>
        <documentation>Name of the configuration to be used. URL, port and credentials are retrieved from the given configuration. To view or create a new configuration, go to the Administration -> Plugins tab, and select 'Configure' action for @PLUGIN_KEY@ plugin.</documentation>
    </formElement>
    <formElement>
        <type>entry</type>
        <label>JBoss controller location:</label>
        <property>jboss_url</property>
        <value></value>
        <documentation>JBoss controller location. For example: localhost:9999. You can find this information in "$JBOSS_HOME/bin/jboss-cli.xml". If secured connection is being used, see <a href="/commander/pages/@PLUGIN_NAME@/@PLUGIN_KEY@_help#known-issues" target="_BLANK">Known Issues</a> section in the documentation.
        </documentation>
        <required>1</required>
    </formElement>
    <formElement>
        <type>entry</type>
        <label>Physical location of the jboss client script:</label>
        <property>scriptphysicalpath</property>
        <value></value>
        <documentation>Provide the physical location of the jboss Command Line Interface script, i.e: 'jboss-cli.bat', '/path/to/jboss-cli.sh'.</documentation>
        <required>0</required>
    </formElement>
    <formElement>
        <supportsCredentialReference>true</supportsCredentialReference>
        <type>credential</type>
        <label>Login as:</label>
        <property>credential</property>
        <value></value>
        <required>0</required>
    </formElement>
    <formElement>
        <type>checkbox</type>
        <label>Check Connection?:</label>
        <property>test_connection</property>
        <checkedValue>1</checkedValue>
        <uncheckedValue>0</uncheckedValue>
        <required>0</required>
        <documentation>If checked, the configuration will be saved only if connection with JBoss can be established with given configuration.</documentation>
    </formElement>
    <formElement>
        <type>entry</type>
        <label>Check Connection Resource:</label>
        <property>test_connection_res</property>
        <value></value>
        <documentation>A resource which is used for checking connection.</documentation>
        <required>0</required>
        <dependsOn>test_connection</dependsOn>
        <condition>${test_connection} == 1</condition>
    </formElement>
    <formElement>
        <type>select</type>
        <label>Log Level:</label>
        <property>log_level</property>
        <value>1</value>
        <option>
            <name>INFO</name>
            <value>1</value>
        </option>
        <option>
            <name>WARNING</name>
            <value>2</value>
        </option>
        <option>
            <name>ERROR</name>
            <value>3</value>
        </option>
        <option>
            <name>DEBUG</name>
            <value>4</value>
        </option>
        <documentation>Log level to use for logging output. INFO=1, WARNING=2, ERROR=3, DEBUG=4.</documentation>
        <required>0</required>
    </formElement>
    <formElement>
        <type>entry</type>
        <label>Additional Java options:</label>
        <property>java_opts</property>
        <value></value>
        <documentation>Additional java options. This line will be added to the value of JAVA_OPTS environment variable.</documentation>
        <required>0</required>
    </formElement>
</editor>
