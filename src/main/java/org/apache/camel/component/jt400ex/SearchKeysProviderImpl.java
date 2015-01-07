package org.apache.camel.component.jt400ex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.*;
import java.util.logging.Level;

/**
 * An implementation of SearchKeysProvider.
 *
 * Include a reference to this class in the beans.xml file with the entry
 *
 * <bean id="searchKeysProvider" class="org.apache.camel.component.jt400ex.SearchKeysProviderImpl"/>
 *
 * Then use the same bean id when defining the endpoint like
 *
 * <camel:route>
 * <camel:from uri="jt400ex://user:password@server/QUEUENAME?keyed=true&amp;searchKeysProvider=searchKeysProvider"/>
 * <camel:to uri="activemq:topic:JMSOutput"/>
 * </camel:route>
 */
@Configuration
@EnableScheduling
public class SearchKeysProviderImpl implements SearchKeysProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchKeysProviderImpl.class);
    private static final int CLEANUP_INTERVAL = 60000;

    final Map<String, Date> keys = new HashMap<String, Date>();

    public SearchKeysProviderImpl() {
        LOGGER.info("Constructed SearchKeysProvider");
    }

    @Override
    synchronized public void addKey(final String key) {
        keys.put(key, new Date());
    }

    @Override
    synchronized public void removeKey(final String key) {
        keys.remove(key);
    }

    @Override
    synchronized public Set<String> getKeys() {
        return Collections.unmodifiableSet(keys.keySet());
    }

    /**
     * Every minute loop over the list of keys and remove any that have not been refreshed
     */
    @Scheduled(fixedRate=CLEANUP_INTERVAL)
    synchronized private void cleanUpOldKeys() {
        final Date now = new Date();

        final List<String> removeList = new ArrayList<String>();

        for (final String key : keys.keySet()) {
            final Date date = keys.get(key);
            if (now.getTime() - date.getTime() > CLEANUP_INTERVAL) {
                removeList.add(key);
            }
        }

        for (final String key : removeList) {
            LOGGER.info("Removed key " + key + " from the list");
            keys.remove(key);
        }
    }
}
