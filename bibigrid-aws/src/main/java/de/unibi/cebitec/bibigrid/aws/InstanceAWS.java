package de.unibi.cebitec.bibigrid.aws;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import de.unibi.cebitec.bibigrid.core.model.Configuration;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public final class InstanceAWS extends de.unibi.cebitec.bibigrid.core.model.Instance {
    private final Instance internalInstance;

    InstanceAWS(Configuration.InstanceConfiguration instanceConfiguration, Instance internalInstance) {
        super(instanceConfiguration);
        this.internalInstance = internalInstance;
    }

    Instance getInternal() {
        return internalInstance;
    }

    @Override
    public String getPublicIp() {
        return internalInstance.getPublicIpAddress();
    }

    @Override
    public String getPrivateIp() {
        return internalInstance.getPrivateIpAddress();
    }

    @Override
    public String getHostname() {
        // TODO: or getPublicDnsName?
        return internalInstance.getPrivateDnsName();
    }

    @Override
    public String getId() {
        return internalInstance.getInstanceId();
    }

    @Override
    public String getName() {
        return internalInstance.getInstanceId();
    }

    @Override
    public String getTag(String key) {
        for (Tag tag : internalInstance.getTags()) {
            if (tag.getKey().equalsIgnoreCase(key)) {
                return tag.getValue();
            }
        }
        return null;
    }

    public String getState() {
        return internalInstance.getState().getName();
    }

    @Override
    public String getKeyName() {
        return internalInstance.getKeyName();
    }

    @Override
    public ZonedDateTime getCreationTimestamp() {
        // AWS returns the launch time as zulu time
        ZonedDateTime creationDateTime = ZonedDateTime.ofInstant(internalInstance.getLaunchTime().toInstant(),
                ZoneId.of("Z").normalized());
        return creationDateTime.withZoneSameInstant(ZoneOffset.systemDefault().normalized());
    }
}
