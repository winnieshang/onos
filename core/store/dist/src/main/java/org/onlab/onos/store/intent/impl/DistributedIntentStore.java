package org.onlab.onos.store.intent.impl;

import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentEvent;
import org.onlab.onos.net.intent.IntentId;
import org.onlab.onos.net.intent.IntentState;
import org.onlab.onos.net.intent.IntentStore;
import org.onlab.onos.net.intent.IntentStoreDelegate;
import org.onlab.onos.store.AbstractStore;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.onlab.onos.net.intent.IntentState.*;
import static org.slf4j.LoggerFactory.getLogger;

//FIXME: I LIE I AM NOT DISTRIBUTED
@Component(immediate = true)
@Service
public class DistributedIntentStore
        extends AbstractStore<IntentEvent, IntentStoreDelegate>
        implements IntentStore {

    private final Logger log = getLogger(getClass());
    private final Map<IntentId, Intent> intents = new ConcurrentHashMap<>();
    private final Map<IntentId, IntentState> states = new ConcurrentHashMap<>();
    private final Map<IntentId, List<Intent>> installable = new ConcurrentHashMap<>();

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public IntentEvent createIntent(Intent intent) {
        intents.put(intent.id(), intent);
        return this.setState(intent, IntentState.SUBMITTED);
    }

    @Override
    public IntentEvent removeIntent(IntentId intentId) {
        Intent intent = intents.remove(intentId);
        installable.remove(intentId);
        IntentEvent event = this.setState(intent, WITHDRAWN);
        states.remove(intentId);
        return event;
    }

    @Override
    public long getIntentCount() {
        return intents.size();
    }

    @Override
    public Iterable<Intent> getIntents() {
        return ImmutableSet.copyOf(intents.values());
    }

    @Override
    public Intent getIntent(IntentId intentId) {
        return intents.get(intentId);
    }

    @Override
    public IntentState getIntentState(IntentId id) {
        return states.get(id);
    }

    @Override
    public IntentEvent setState(Intent intent, IntentState state) {
        IntentId id = intent.id();
        states.put(id, state);
        IntentEvent.Type type = (state == SUBMITTED ? IntentEvent.Type.SUBMITTED :
                (state == INSTALLED ? IntentEvent.Type.INSTALLED :
                        (state == FAILED ? IntentEvent.Type.FAILED :
                                state == WITHDRAWN ? IntentEvent.Type.WITHDRAWN :
                                        null)));
        return type == null ? null : new IntentEvent(type, intent);
    }

    @Override
    public void addInstallableIntents(IntentId intentId, List<Intent> result) {
        installable.put(intentId, result);
    }

    @Override
    public List<Intent> getInstallableIntents(IntentId intentId) {
        return installable.get(intentId);
    }

    @Override
    public void removeInstalledIntents(IntentId intentId) {
        installable.remove(intentId);
    }

}