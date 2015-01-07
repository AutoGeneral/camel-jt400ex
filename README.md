INTRODUCTION
============

JT400 keyed data queues have a fairly limited number of ways that they can be accessed from Camel. Because the name of
the key needs to be added to the endpoint url, you need to know the key names beforehand, or use keys that can
be matched with the various "search types" like GT, LT, GE, LE, NE.

If you do not know the names of the keys then the existing jt400 endpoint is of little value, because there is no way
to know what key needs to be added to the url. You could generate the routes at runtime, but then you have multiple
consumers polling the server, which is not very efficient.

This library is an modification of the original jt400: consumer. Instead of accepting a key, it accepts the name
of a Spring bean that implements the SearchKeysProvider interface. The consumer will then loop over each key added
to the SearchKeysProvider object, returning any matching messages.

Keys can be added to the SearchKeysProvider object at runtime. For convenience we have the AddSerachKeyProcessor,
which also accepts a reference to the Spring SearchKeysProvider bean, and adds new keys based on the contents
of any messages that it processes.

ADDITIONAL JARS
===============

You'll need to make sure camel has access to jt400-full-6.0.jar and quick-json-1.0.4.jar.

CONFIGURATION
=============
Enable scheduled beans by ensuring you have the task namespace defined in the bean element

```xml
﻿<beans
    xmlns="http://www.springframework.org/schema/beans"
    xmlns:amq="http://activemq.apache.org/schema/core"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:camel="http://camel.apache.org/schema/spring"
    xmlns:task="http://www.springframework.org/schema/task"
    xsi:schemaLocation="http://www.springframework.org/schema/beans
      http://www.springframework.org/schema/beans/spring-beans-2.0.xsd
      http://activemq.apache.org/schema/core
      http://activemq.apache.org/schema/core/activemq-core.xsd
      http://camel.apache.org/schema/spring
      http://camel.apache.org/schema/spring/camel-spring.xsd
      http://www.springframework.org/schema/task
      http://www.springframework.org/schema/task/spring-task-3.0.xsd">
```

Then define the scheduler

```xml
<task:annotation-driven executor="myExecutor" scheduler="myScheduler"/>
<task:executor id="myExecutor" pool-size="5"/>
<task:scheduler id="myScheduler" pool-size="10"/>
```

You'll then need to define the various processors and endpoints.

```xml
<!-- This is our instance of a class that extends the SearchKeysProvider interface -->
<bean id="searchKeysProvider" class="org.apache.camel.component.jt400ex.SearchKeysProviderImpl"/>

<!-- This is the processor that adds keys to the SearchKeysProvider object -->
<bean id="addSearchKeyProcessor" class="org.apache.camel.component.jt400ex.AddSearchKeyProcessor">
    <property name="searchKeysProvider" ref="searchKeysProvider"/>
</bean>

<!--
    This is the processor that takes a JSON message from an AJAX client, extracts the headers and message,
    and forwards it along to another queue. This is required because AJAX clients can't set headers,
    and the keyed data queue needs the header "key" to be set.

    An example message looks like

    {
        "headers":[{"name":"key", "value":"KEYNAME"}],
        "message":'{"type":"policy","message":"hi"}'
    }
-->
<bean id="ajaxWithHeadersProcessor" class="org.apache.camel.ajax.AjaxWithHeadersProcessor"/>

<camel:camelContext id="camel">
    <camel:route>
        <camel:from uri="jt400ex://user:password@server/SOMELIB.LIB/SOMEQUEUE.DTAQ?keyed=true&amp;searchKeysProvider=searchKeysProvider"/>
        <camel:to uri="activemq:topic:OutMessages"/>
    </camel:route>
    <camel:route>
        <camel:from uri="activemq:queue:InMessages"/>
        <camel:to uri="jt400ex://user:password@server/SOMELIB.LIB/SOMEQUEUE.DTAQ?keyed=true"/>
    </camel:route>
    <!-- We use the JMS AddKeyQueue queue to register new keys to be removed from the JT400 keyed data queue -->
    <camel:route>
        <camel:from uri="activemq:queue:AddKeyQueue"/>
        <camel:process ref="addSearchKeyProcessor"/>
    </camel:route>
    <!-- This queue accepts the specially formatted JSON messages and passes them along to the keyed data queue -->
    <camel:route>
        <camel:from uri="activemq:queue:DISCIntegerationAJAXIn"/>
        <camel:process ref="ajaxWithHeadersProcessor"/>
        <camel:to uri="jt400ex://user:password@server/SOMELIB.LIB/SOMEQUEUE.DTAQ?keyed=true"/>
    </camel:route>
</camel:camelContext>
```