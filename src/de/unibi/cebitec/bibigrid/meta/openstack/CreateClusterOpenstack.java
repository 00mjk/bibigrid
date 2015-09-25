/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.meta.openstack;

import com.amazonaws.services.ec2.model.BlockDeviceMapping;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import de.unibi.cebitec.bibigrid.meta.CreateCluster;
import static de.unibi.cebitec.bibigrid.meta.aws.CreateClusterAWS.log;
import de.unibi.cebitec.bibigrid.model.Configuration;
import de.unibi.cebitec.bibigrid.util.DeviceMapper;
import de.unibi.cebitec.bibigrid.util.InstanceInformation;
import de.unibi.cebitec.bibigrid.util.KEYPAIR;
import de.unibi.cebitec.bibigrid.util.UserDataCreator;
import static de.unibi.cebitec.bibigrid.util.VerboseOutputFilter.V;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jclouds.ContextBuilder;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Flavor;
import org.jclouds.openstack.nova.v2_0.domain.Image;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.domain.ServerCreated;
import org.jclouds.openstack.nova.v2_0.features.FlavorApi;
import org.jclouds.openstack.nova.v2_0.features.ImageApi;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.openstack.nova.v2_0.options.CreateServerOptions;
import org.jclouds.sshj.config.SshjSshClientModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jsteiner
 */
public class CreateClusterOpenstack implements CreateCluster<CreateClusterOpenstack, CreateClusterEnvironmentOpenstack> {

    public static final Logger log = LoggerFactory.getLogger(CreateClusterOpenstack.class);

    private final String endPoint;
//    private final ComputeServiceContext context;
    private final NovaApi novaClient;

    private final String os_region = "regionOne";

    private final String provider = "openstack-nova", username = "bibiserv:bibiserv", password = "OpenstackBibiserv";

    private CreateClusterEnvironmentOpenstack environment;

    private Configuration conf;

    public static final String PREFIX = "bibigrid-";
    public static final String SECURITY_GROUP_PREFIX = PREFIX + "sg-";

    public static final String MASTER_SSH_USER = "ubuntu";
    public static final String PLACEMENT_GROUP_PREFIX = PREFIX + "pg-";
    public static final String SUBNET_PREFIX = PREFIX + "subnet-";

    private String base64MasterUserData;

    private List<BlockDeviceMapping> masterDeviceMappings;
    private String clusterId;
    private DeviceMapper slaveDeviceMapper;
    private List<BlockDeviceMapping> slaveBlockDeviceMappings;

    private KEYPAIR keypair;

    public CreateClusterOpenstack(Configuration conf) {

        this.conf = conf;
        this.endPoint = conf.getOpenstackEndpoint();
        Iterable<Module> modules = ImmutableSet.<Module>of(
                new SshjSshClientModule(),
                new SLF4JLoggingModule());

//        context = ContextBuilder.newBuilder(provider)
//                .endpoint(this.endPoint)
//                .credentials(username, password)
//                .modules(modules)
//                .buildView(ComputeServiceContext.class);
        novaClient = ContextBuilder.newBuilder(provider)
                .endpoint(endPoint)
                .credentials(conf.getOpenstackCredentials().getTenantName() + ":" + conf.getOpenstackCredentials().getUsername(), conf.getOpenstackCredentials().getPassword())
                .modules(modules)
                .buildApi(NovaApi.class);
        log.info("Openstack connection established ...");
    }

    @Override
    public CreateClusterEnvironmentOpenstack createClusterEnvironment() {
        return environment = new CreateClusterEnvironmentOpenstack(this);
    }

    @Override
    public CreateClusterOpenstack configureClusterMasterInstance() {
        log.info("Master instance configuration not implemented yet ...");
        log.info("Going to use a hardcoded instance type ...");
        return this;
    }

    @Override
    public CreateClusterOpenstack configureClusterSlaveInstance() {
        log.error("Slave configuration not supported yet!");
        return this;
    }

    @Override
    public boolean launchClusterInstances() {
        try {
            ServerApi s = novaClient.getServerApi(os_region);
            List<Image> images = listImages();
            List<Flavor> flavors = listFlavors();
            /**
             * Options.
             */
            CreateServerOptions opt = new CreateServerOptions();
            opt.keyPairName(conf.getKeypair());

            String image = os_region + "/" + conf.getMasterImage();
            String type = conf.getMasterInstanceType().toString();
            Flavor selectedFlavor = null;
            for (Flavor f : flavors) {
                if (f.getName().equals(type)) {
                    selectedFlavor = f;
                }
            }
            ServerCreated created = s.create("bibigrid_test", image, selectedFlavor.getId(), opt);
            log.info("Instance (ID: {}) successfully started", created.getId());
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private List<Server> listServers() {
        List<Server> ret = new ArrayList<>();
        Set<String> regions = novaClient.getConfiguredRegions();
        for (String region : regions) {
            ServerApi serverApi = novaClient.getServerApi(region);

            System.out.println("Servers in " + region);

            for (Server server : serverApi.listInDetail().concat()) {
                ret.add(server);
            }
        }
        return ret;
    }

    private List<Flavor> listFlavors() {
        List<Flavor> ret = new ArrayList<>();
        FlavorApi f = novaClient.getFlavorApi(os_region); // hardcoded
        for (Flavor r : f.listInDetail().concat()) {
            ret.add(r);
        }
        return ret;
    }

    private List<Image> listImages() {
        List<Image> ret = new ArrayList<>();
        ImageApi imageApi = novaClient.getImageApi(os_region);
        for (Image m : imageApi.listInDetail().concat()) {
            ret.add(m);
        }
        return ret;
    }

}
