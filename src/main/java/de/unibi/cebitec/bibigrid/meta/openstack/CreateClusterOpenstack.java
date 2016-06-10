/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.meta.openstack;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import de.unibi.cebitec.bibigrid.meta.CreateCluster;
import de.unibi.cebitec.bibigrid.model.Configuration;
import de.unibi.cebitec.bibigrid.util.DeviceMapper;
import static de.unibi.cebitec.bibigrid.util.ImportantInfoOutputFilter.I;
import de.unibi.cebitec.bibigrid.util.JSchLogger;
import de.unibi.cebitec.bibigrid.util.SshFactory;
import de.unibi.cebitec.bibigrid.util.UserDataCreator;
import static de.unibi.cebitec.bibigrid.util.VerboseOutputFilter.V;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.codec.binary.Base64;
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Address;
import org.openstack4j.model.compute.Addresses;
import org.openstack4j.model.compute.BDMDestType;
import org.openstack4j.model.compute.BDMSourceType;
import org.openstack4j.model.compute.BlockDeviceMappingCreate;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Image;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.ServerCreate;
import org.openstack4j.model.network.NetFloatingIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jan Krueger - jkrueger (at)cebitec.uni-bielfeld.de 1st version by
 * Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
public class CreateClusterOpenstack extends OpenStackIntent implements CreateCluster<CreateClusterOpenstack, CreateClusterEnvironmentOpenstack> {
    
    public static final Logger log = LoggerFactory.getLogger(CreateClusterOpenstack.class);

//    private final NovaApi novaApi;
//    private final ServerApi serverApi;
//    private final SecurityGroupApi securityGroupApi;
//    private final FlavorApi flavorApi;
//    private final ImageApi imageApi;
//    private final FloatingIPApi floatingApi;
//    private final NeutronApi neutronApi;
//    private final SubnetApi subnetApi;
    private final String os_region;
    
    private final String provider = "openstack-nova";
    
    private CreateClusterEnvironmentOpenstack environment;
    

    
    public static final String PREFIX = "bibigrid-";
    public static final String SECURITY_GROUP_PREFIX = PREFIX + "sg-";
    
    public static final String MASTER_SSH_USER = "ubuntu";
    public static final String PLACEMENT_GROUP_PREFIX = PREFIX + "pg-";
    public static final String SUBNET_PREFIX = PREFIX + "subnet-";
    
    private String clusterId;

    /**
     * MetaData
     */
    private Map<String, String> metadata;
    
 
    
    public CreateClusterOpenstack(Configuration conf) {
        super(conf);
        // MetaData
        metadata = new HashMap<>();
        metadata.put("user", conf.getUser());

        // Cluster ID is a cut down base64 encoded version of a random UUID:
        UUID clusterIdUUID = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(clusterIdUUID.getMostSignificantBits());
        bb.putLong(clusterIdUUID.getLeastSignificantBits());
        String clusterIdBase64 = Base64.encodeBase64URLSafeString(bb.array()).replace("-", "").replace("_", "");
        int len = clusterIdBase64.length() >= 15 ? 15 : clusterIdBase64.length();
        clusterId = clusterIdBase64.substring(0, len);
        log.debug("cluster id: {}", clusterId);
        
        this.conf = conf;
        os_region = conf.getRegion();

//        Iterable<Module> modules = ImmutableSet.<Module>of(
//                new SshjSshClientModule(),
//                new SLF4JLoggingModule());
//        novaApi = ContextBuilder.newBuilder(provider)
//                .endpoint(conf.getOpenstackCredentials().getEndpoint())
//                .credentials(conf.getOpenstackCredentials().getTenantName() + ":" + conf.getOpenstackCredentials().getUsername(), conf.getOpenstackCredentials().getPassword())
//                .modules(modules)
//                .buildApi(NovaApi.class);
//
//        neutronApi = ContextBuilder.newBuilder(new NeutronApiMetadata())
//                .endpoint(conf.getOpenstackCredentials().getEndpoint())
//                .credentials(conf.getOpenstackCredentials().getTenantName() + ":" + conf.getOpenstackCredentials().getUsername(), conf.getOpenstackCredentials().getPassword())
//                .buildApi(NeutronApi.class);
//        serverApi = novaApi.getServerApi(os_region);
//        securityGroupApi = novaApi.getSecurityGroupApi(os_region).get(); // @ToDo : that may fail !!!
//        flavorApi = novaApi.getFlavorApi(os_region);
//        imageApi = novaApi.getImageApi(os_region);
//        floatingApi = novaApi.getFloatingIPApi(os_region).get(); //@ToDo: that may fail !!!
//
//        subnetApi = neutronApi.getSubnetApi(os_region);
        log.info("Openstack connection established ...");
    }
    
    @Override
    public CreateClusterEnvironmentOpenstack createClusterEnvironment() {
        return environment = new CreateClusterEnvironmentOpenstack(this);
    }
    
    private Flavor masterFlavor, slaveFlavor;
    //private CreateServerOptions masterOptions, slaveOptions;  -> os.compute()
    private String masterImage, slaveImage;
    private DeviceMapper masterDeviceMapper, slaveDeviceMapper;
    private Set<BlockDeviceMappingCreate> masterMappings, slaveMappings;
    
    @Override
    public CreateClusterOpenstack configureClusterMasterInstance() {
        List<Flavor> flavors = listFlavors();

        /**
         * DeviceMapper init.
         *
         */
        Map<String, String> masterSnapshotToMountPointMap = this.conf.getMasterMounts();
        masterDeviceMapper = new DeviceMapper(masterSnapshotToMountPointMap, conf.getMasterInstanceType().getSpec().ephemerals);

        /**
         * BlockDeviceMapping.
         *
         */
        masterMappings = new HashSet<>();
        String[] ephemerals = {"b", "c", "d", "e"};
        for (int i = 0; i < this.conf.getMasterInstanceType().getSpec().ephemerals; ++i) {
            BlockDeviceMappingCreate bdmc = Builders.blockDeviceMapping()
                    .deviceName("sd" + ephemerals[i])
                    .deleteOnTermination(true)
                    .sourceType(BDMSourceType.BLANK)
                    .destinationType(BDMDestType.LOCAL)
                    .bootIndex(-1)
                    .build();
            masterMappings.add(bdmc);
        }
        
        masterImage = os_region + "/" + conf.getMasterImage();
        
        String type = conf.getMasterInstanceType().getValue();
        masterFlavor = null;
        for (Flavor f : flavors) {
            if (f.getName().equals(type)) {
                masterFlavor = f;
                break;
            }
        }
        log.info("Master configured");
        return this;
    }
    
    @Override
    public CreateClusterOpenstack configureClusterSlaveInstance() {
        List<Flavor> flavors = listFlavors();

        /**
         * DeviceMapper Slave.
         *
         * @ToDo
         */
        Map<String, String> snapShotToSlaveMounts = this.conf.getSlaveMounts();
        slaveDeviceMapper = new DeviceMapper(snapShotToSlaveMounts, conf.getMasterInstanceType().getSpec().ephemerals);

        /**
         * BlockDeviceMapping.
         *
         * @ToDo
         */
        slaveMappings = new HashSet<>();
        String[] ephemerals = {"b", "c", "d", "e"};
        for (int i = 0; i < this.conf.getSlaveInstanceType().getSpec().ephemerals; ++i) {
            BlockDeviceMappingCreate bdmc = Builders.blockDeviceMapping()
                    .deviceName("sd" + ephemerals[i])
                    .deleteOnTermination(true)
                    .sourceType(BDMSourceType.BLANK)
                    .destinationType(BDMDestType.LOCAL)
                    .bootIndex(-1)
                    .build();
            slaveMappings.add(bdmc);
            
        }

        /**
         * Options.
         */
        slaveImage = os_region + "/" + conf.getSlaveImage();
        String type = conf.getSlaveInstanceType().getValue();
        slaveFlavor = null;
        
        for (Flavor f : flavors) {
            if (f.getName().equals(type)) {
                slaveFlavor = f;
                break;
            }
        }
        log.info("Slave(s) configured");
        return this;
    }
    
    private final Instance master = new Instance();
    private final List<Instance> slaves = new ArrayList<>();
    
    @Override
    public boolean launchClusterInstances() {
        try {
            NetFloatingIP floatingip = getFloatingIP();
            
            if (floatingip == null) {
                log.error("No  unused floating ip available! Abbort!");
                return false;
            }

//            ServerCreated createdMaster = serverApi.create("bibigrid-master-" + clusterId, masterImage, masterFlavor.getId(), masterOptions);
            //masterOptions = new CreateServerOptions();
//        masterOptions.keyPairName(conf.getKeypair());
//        masterOptions.securityGroupNames(environment.getSecurityGroup().getName());
//        masterOptions.availabilityZone(conf.getAvailabilityZone());
//        masterOptions.userData(UserDataCreator.masterUserData(masterDeviceMapper, conf, environment.getKeypair()).getBytes()); //fails cause of null devicemapper
//        masterOptions.metadata(metadata);
            ServerCreate sc = Builders.server()
                    .name("bibigrid-master-" + clusterId)
                    .flavor(masterFlavor.getId())
                    .image(masterImage)
                    .keypairName(conf.getKeypair())
                    .addSecurityGroup(environment.getSecGroupExtension().getName())
                    .availabilityZone(conf.getAvailabilityZone())
                    .userData(UserDataCreator.masterUserData(masterDeviceMapper, conf, environment.getKeypair()))
                    .addMetadata(metadata)
                    //.addPersonality("/etc/motd", "Welcome to the new VM! Restricted access only")
                    .build();
            Server server = os.compute().servers().bootAndWaitActive(sc,60000);
            
            log.info("Master (ID: {}) started", server.getId());
            
            master.setId(server.getId());
            master.setIp(server.getAddresses().getAddresses(conf.getNetworkname()).get(0).getAddr()); // @ToDo
            master.setPublicIp(floatingip.getFloatingIpAddress());
            master.setHostname("bibigrid-master-" + clusterId);
            master.updateNeutronHostname();
            
            log.info("Master (ID: {}) network configuration finished", master.getId());
            
            os.compute().floatingIps().addFloatingIP(server, floatingip.getFloatingIpAddress());

            //floatingApi.addToServer(floatingip.getIp(), createdMaster.getId());
            log.info("FloatingIP {} assigned to Master(ID: {}) ", master.getPublicIp(), master.getId());
            

            //start all slave instances  
            for (int i = 0; i < conf.getSlaveInstanceCount(); i++) {
                // ServerCreated createdSlave = serverApi.create("bibigrid-slave-" + (i + 1) + "-" + clusterId, slaveImage, slaveFlavor.getId(), slaveOptions);
                sc = Builders.server()
                        .name("bibigrid-slave-" + (i + 1) + "-" + clusterId)
                        .flavor(slaveFlavor.getId())
                        .image(slaveImage)
                        .keypairName(conf.getKeypair())
                        .addSecurityGroup(environment.getSecGroupExtension().getId())
                        .availabilityZone(conf.getAvailabilityZone())
                        .userData(UserDataCreator.forSlave(master.getIp(),
                                master.getNeutronHostname(),
                                slaveDeviceMapper,
                                conf,
                                environment.getKeypair()))
                        .build();
                
                Server si = os.compute().servers().bootAndWaitActive(sc,60000);
                
                Instance slave = new Instance();
                slave.setId(si.getId());
                slave.setHostname(si.getName());
                slaves.add(slave);
            }
            // wait for slave network finished ... update server instance list            
            for (Instance i : slaves) {
                Address addr = getLocalIp(i.getId(), "bibiserv"); // @ToDo : HardCoded Network !!!!
                i.setIp(addr.getAddr());
                i.updateNeutronHostname();
            }
            log.info("Cluster (ID: {}) successfully created!", clusterId);
            
            sshTestAndExecute();

            ////////////////////////////////////
            //// Human friendly output
            StringBuilder sb = new StringBuilder();
            sb.append("\n You might want to set the following environment variable:\n\n");
            sb.append("export BIBIGRID_MASTER=").append(master.getPublicIp()).append("\n\n");
            sb.append("You can then log on the master node with:\n\n")
                    .append("ssh -i ")
                    .append(conf.getIdentityFile())
                    .append(" ubuntu@$BIBIGRID_MASTER\n\n");
            sb.append("The cluster id of your started cluster is : ")
                    .append(clusterId)
                    .append("\n\n");
            sb.append("The can easily terminate the cluster at any time with :\n")
                    .append("./bibigrid -t ").append(clusterId).append(" ");
            if (conf.isAlternativeConfigFile()) {
                sb.append("-o ").append(conf.getAlternativeConfigPath()).append(" ");
            }
            
            sb.append("\n");
            
            log.info(sb.toString());
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Return first network address object of given instance (by its ID and
     * network name). Block until address is available.
     *
     * @param instanceID - ID of given instances
     * @param network - Network name
     *
     * @return Address
     */
    private Address getLocalIp(String instanceID, String network) {
        Address addr = null;
        do {
            Server instance = os.compute().servers().get(instanceID);
            if (instance != null && instance.getAddresses().getAddresses(network).size() > 0) {
                addr = instance.getAddresses().getAddresses(network).iterator().next();
            } else {
                try {
                    Thread.sleep(2500);
                    System.out.print(".");
                } catch (InterruptedException ex) {
                    log.error("Can't sleep!");
                }
            }
        } while (addr == null);
        return addr;
    }
    
    private void sshTestAndExecute() {
        JSch ssh = new JSch();
        JSch.setLogger(new JSchLogger());
        /*
         * Building Command
         */
        log.info("Now configuring ...");
        String execCommand = SshFactory.buildSshCommandOpenstack(clusterId, this.getConfiguration(), master, slaves);
        log.info(V, "Building SSH-Command : {}", execCommand);
        
        boolean uploaded = false;
        boolean configured = false;
        
        int ssh_attempts = 25; // @TODO attempts
        while (!configured && ssh_attempts > 0) {
            try {
                
                ssh.addIdentity(this.getConfiguration().getIdentityFile().toString());
                log.info("Trying to connect to master ({})...", ssh_attempts);
                Thread.sleep(4000);

                /*
                 * Create new Session to avoid packet corruption.
                 */
                Session sshSession = SshFactory.createNewSshSession(ssh, master.getPublicIp(), MASTER_SSH_USER, getConfiguration().getIdentityFile());

                /*
                 * Start connect attempt
                 */
                sshSession.connect();
                log.info("Connected to master!");
                ChannelExec channel = (ChannelExec) sshSession.openChannel("exec");
                
                BufferedReader stdout = new BufferedReader(new InputStreamReader(channel.getInputStream()));
                BufferedReader stderr = new BufferedReader(new InputStreamReader(channel.getErrStream()));
                
                channel.setCommand(execCommand);
                
                log.info(V, "Connecting ssh channel...");
                channel.connect();
                
                String lineout = null, lineerr = null;
                
                while (!configured) {
                    
                    if (stdout.ready()) {                        
                        lineout = stdout.readLine();
                        if (lineout.equals("CONFIGURATION_FINISHED")) {
                            configured = true;
                        }
                        log.info(V, "SSH: {}", lineout);
                    }
                    
                    if (stderr.ready()) {
                        lineerr = stderr.readLine();
                        log.error("SSH: {}", lineerr);
                    }
                    
                    Thread.sleep(500);
                }
                if (channel.isClosed()) {
                    log.info(V, "SSH: exit-status: {}", channel.getExitStatus());
                }
                channel.disconnect();
                sshSession.disconnect();
                
            } catch (IOException | JSchException e) {
                ssh_attempts--;
                if (ssh_attempts == 0) {
                    log.error(V, "SSH: {}", e);
                }
                
            } catch (InterruptedException ex) {
                log.warn("Interrupted ...");
            }
        }
        
        if (configured) {
            log.info(I, "Master instance has been configured.");
        } else {
            log.error("Master instance configuration failed!");
        }
        
    }
    
    private List<Server> listServers(String region) {
        List<Server> ret = new ArrayList<>();
        
        for (Server server : os.compute().servers().list()) {
            ret.add(server);
        }
        return ret;
    }
    
    private List<Flavor> listFlavors() {
        List<Flavor> ret = new ArrayList<>();
        for (Flavor r : os.compute().flavors().list()) {
            ret.add(r);
        }
        return ret;
    }
    
    private List<Image> listImages() {
        List<Image> ret = new ArrayList<>();
        for (Image m : os.compute().images().list()) {
            ret.add(m);
        }
        return ret;
    }

    /**
     * Returns an available (not used) FloatingIP. Returns null in the case that
     * no unused FloatingIP is available and no new FloatingIP could be
     * allocated.
     *
     * @return
     */
    private NetFloatingIP getFloatingIP() {

        // get list of all available floating IP's, and search for a free one
        List<? extends NetFloatingIP> l = os.networking().floatingip().list();
        
        for (int i = 0; i < l.size(); i++) {
            NetFloatingIP fip = l.get(i);
            if (fip.getPortId() == null) {
                //found an unused floating ip and return it
                return fip;
            }
        }
        // try to allocate a new floating from network pool
//        try {
//            return floatingApi.allocateFromPool("ext-cebitec"); // @ToDo hardcoded
//        } catch (Exception e) {
//            log.error(e.getMessage());
//        }
        return null;
        
    }

//    public NovaApi getNovaApi() {
//        return novaApi;
//    }
//
//    public NeutronApi getNeutronApi() {
//        return neutronApi;
//    }
//
//    public ServerApi getServerApi() {
//        return serverApi;
//    }
//
//    public SecurityGroupApi getSecurityGroupApi() {
//        return securityGroupApi;
//    }
//
//    public FlavorApi getFlavorApi() {
//        return flavorApi;
//    }
//
//    public SubnetApi getSubnetApi() {
//        return subnetApi;
//    }
//
//    public ImageApi getImageApi() {
//        return imageApi;
//    }
    public String getClusterId() {
        return clusterId;
    }
    
    public Configuration getConfiguration() {
        return this.conf;
    }
    
    public class Instance {
        
        private String id, ip, publicIp;
        
        private String hostname, neutronHostname;
        
        public Instance() {
        }
        
        ;

       
       public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getIp() {
            return ip;
        }
        
        public void setIp(String ip) {
            this.ip = ip;
        }
        
        public String getPublicIp() {
            return publicIp;
        }
        
        public void setPublicIp(String publicIp) {
            this.publicIp = publicIp;
        }
        
        public String getHostname() {
            return hostname;
        }
        
        public void setHostname(String hostname) {
            this.hostname = hostname;
        }
        
        public String getNeutronHostname() {
            return neutronHostname;
        }
        
        public void setNeutronHostname(String neutronHostname) {
            this.neutronHostname = neutronHostname;
        }
        
        public void updateNeutronHostname() {
            if (ip != null && ip.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
                String[] t = ip.split("\\.");
                neutronHostname = "host-" + String.join("-", t);
            } else {
                log.warn("ip must be a valid IPv4 address string.");
            }
        }
        
    }
    
    public OSClient getOs() {
        return os;
    }
    
}