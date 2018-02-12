package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.CommandLineValidator;
import de.unibi.cebitec.bibigrid.core.model.IntentMode;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.util.DefaultPropertiesFile;
import de.unibi.cebitec.bibigrid.core.util.RuleBuilder;
import org.apache.commons.cli.CommandLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public final class CommandLineValidatorOpenstack extends CommandLineValidator {
    private final ConfigurationOpenstack openstackConfig;

    CommandLineValidatorOpenstack(final CommandLine cl, final DefaultPropertiesFile defaultPropertiesFile,
                                  final IntentMode intentMode, final ProviderModule providerModule) {
        super(cl, defaultPropertiesFile, intentMode, providerModule);
        openstackConfig = (ConfigurationOpenstack) config;
    }

    @Override
    protected Class<ConfigurationOpenstack> getProviderConfigurationClass() {
        return ConfigurationOpenstack.class;
    }

    @Override
    protected List<String> getRequiredOptions() {
        switch (intentMode) {
            case LIST:
                return Arrays.asList(
                        RuleBuilder.RuleNames.KEYPAIR_S.toString(),
                        RuleBuilder.RuleNames.REGION_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_USERNAME_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_TENANT_NAME_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_PASSWORD_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_ENDPOINT_S.toString());
            case TERMINATE:
                return Arrays.asList(
                        "t",
                        RuleBuilder.RuleNames.REGION_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_TENANT_NAME_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_USERNAME_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_PASSWORD_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_ENDPOINT_S.toString());
            case CREATE:
                return Arrays.asList(
                        RuleBuilder.RuleNames.SSH_USER_S.toString(),
                        RuleBuilder.RuleNames.MASTER_INSTANCE_TYPE_S.toString(),
                        RuleBuilder.RuleNames.MASTER_IMAGE_S.toString(),
                        RuleBuilder.RuleNames.SLAVE_INSTANCE_TYPE_S.toString(),
                        RuleBuilder.RuleNames.SLAVE_IMAGE_S.toString(),
                        RuleBuilder.RuleNames.SLAVE_INSTANCE_COUNT_S.toString(),
                        RuleBuilder.RuleNames.KEYPAIR_S.toString(),
                        RuleBuilder.RuleNames.IDENTITY_FILE_S.toString(),
                        RuleBuilder.RuleNames.REGION_S.toString(),
                        RuleBuilder.RuleNames.AVAILABILITY_ZONE_S.toString(),
                        RuleBuilder.RuleNames.USE_MASTER_AS_COMPUTE_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_TENANT_NAME_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_USERNAME_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_PASSWORD_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_ENDPOINT_S.toString());
            case VALIDATE:
                return Arrays.asList(
                        RuleBuilder.RuleNames.OPENSTACK_TENANT_NAME_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_USERNAME_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_PASSWORD_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_ENDPOINT_S.toString());
        }
        return null;
    }

    @Override
    protected boolean validateProviderParameters() {
        return parseCredentialParameters() &&
                parseRouterParameter() &&
                parseNetworkParameter() &&
                parseSecurityGroupParameter();
    }

    private boolean parseCredentialParameters() {
        if (openstackConfig.getOpenstackCredentials() == null) {
            openstackConfig.setOpenstackCredentials(new OpenStackCredentials());
        }
        OpenStackCredentials credentials = openstackConfig.getOpenstackCredentials();
        return parseCredentialsUsernameParameter(credentials) &&
                parseCredentialsDomainParameter(credentials) &&
                parseCredentialsTenantNameParameter(credentials) &&
                parseCredentialsTenantDomainParameter(credentials) &&
                parseCredentialsPasswordParameter(credentials) &&
                parseCredentialsEndpointParameter(credentials);
    }

    private boolean parseCredentialsUsernameParameter(OpenStackCredentials credentials) {
        String shortParam = RuleBuilder.RuleNames.OPENSTACK_USERNAME_S.toString();
        String envParam = RuleBuilder.RuleNames.OPENSTACK_USERNAME_ENV.toString();
        // Parse environment variable if not loaded from config file
        if (isStringNullOrEmpty(credentials.getUsername()) && !isStringNullOrEmpty(System.getenv(envParam))) {
            credentials.setUsername(System.getenv(envParam));
        }
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                credentials.setUsername(value);
            }
        }
        // Validate parameter if required
        if (req.contains(shortParam)) {
            if (isStringNullOrEmpty(credentials.getUsername())) {
                LOG.error("-" + shortParam + " option is required!");
                return false;
            }
        }
        return true;
    }

    private boolean parseCredentialsDomainParameter(OpenStackCredentials credentials) {
        String shortParam = RuleBuilder.RuleNames.OPENSTACK_DOMAIN_S.toString();
        String envParam = RuleBuilder.RuleNames.OPENSTACK_DOMAIN_ENV.toString();
        // Parse environment variable if not loaded from config file
        if (isStringNullOrEmpty(credentials.getDomain()) && !isStringNullOrEmpty(System.getenv(envParam))) {
            credentials.setDomain(System.getenv(envParam));
        }
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                credentials.setDomain(value);
            }
        }
        // Validate parameter if required
        if (req.contains(shortParam)) {
            if (isStringNullOrEmpty(credentials.getDomain())) {
                LOG.error("-" + shortParam + " option is required!");
                return false;
            }
        }
        if (isStringNullOrEmpty(credentials.getDomain())) {
            LOG.info("Keystone V2 API.");
        }
        return true;
    }

    private boolean parseCredentialsTenantNameParameter(OpenStackCredentials credentials) {
        String shortParam = RuleBuilder.RuleNames.OPENSTACK_TENANT_NAME_S.toString();
        String envParam = RuleBuilder.RuleNames.OPENSTACK_TENANT_NAME_ENV.toString();
        // Parse environment variable if not loaded from config file
        if (isStringNullOrEmpty(credentials.getTenantName()) && !isStringNullOrEmpty(System.getenv(envParam))) {
            credentials.setTenantName(System.getenv(envParam));
        }
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                credentials.setTenantName(value);
            }
        }
        // Validate parameter if required
        if (req.contains(shortParam)) {
            if (isStringNullOrEmpty(credentials.getTenantName())) {
                LOG.error("-" + shortParam + " option is required!");
                return false;
            }
        }
        return true;
    }

    private boolean parseCredentialsTenantDomainParameter(OpenStackCredentials credentials) {
        String shortParam = RuleBuilder.RuleNames.OPENSTACK_TENANT_DOMAIN_S.toString();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                credentials.setTenantDomain(value);
            }
        }
        // Validate parameter if required
        if (req.contains(shortParam)) {
            if (isStringNullOrEmpty(credentials.getTenantDomain())) {
                LOG.error("-" + shortParam + " option is required!");
                return false;
            }
        }
        if (isStringNullOrEmpty(credentials.getTenantDomain())) {
            LOG.info("OpenStack TenantDomain is empty! Use OpenStack Domain instead!");
            credentials.setTenantDomain(credentials.getDomain());
        }
        return true;
    }

    private boolean parseCredentialsPasswordParameter(OpenStackCredentials credentials) {
        String shortParam = RuleBuilder.RuleNames.OPENSTACK_PASSWORD_S.toString();
        String envParam = RuleBuilder.RuleNames.OPENSTACK_PASSWORD_ENV.toString();
        // Parse environment variable if not loaded from config file
        if (isStringNullOrEmpty(credentials.getPassword()) && !isStringNullOrEmpty(System.getenv(envParam))) {
            credentials.setPassword(System.getenv(envParam));
        }
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                credentials.setPassword(value);
            }
        }
        // Validate parameter if required
        if (req.contains(shortParam)) {
            if (isStringNullOrEmpty(credentials.getPassword())) {
                LOG.error("-" + shortParam + " option is required!");
                return false;
            }
        }
        return true;
    }

    private boolean parseCredentialsEndpointParameter(OpenStackCredentials credentials) {
        String shortParam = RuleBuilder.RuleNames.OPENSTACK_ENDPOINT_S.toString();
        String envParam = RuleBuilder.RuleNames.OPENSTACK_ENDPOINT_ENV.toString();
        // Parse environment variable if not loaded from config file
        if (isStringNullOrEmpty(credentials.getEndpoint()) && !isStringNullOrEmpty(System.getenv(envParam))) {
            credentials.setEndpoint(System.getenv(envParam));
        }
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                credentials.setEndpoint(value);
            }
        }
        // Validate parameter if required
        if (req.contains(shortParam)) {
            if (isStringNullOrEmpty(credentials.getEndpoint())) {
                LOG.error("-" + shortParam + " option is required!");
                return false;
            }
        }
        return true;
    }

    private boolean parseRouterParameter() {
        final String shortParam = RuleBuilder.RuleNames.ROUTER_S.toString();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                openstackConfig.setRouter(value);
            }
        }
        // Validate parameter if required
        if (req.contains(shortParam)) {
            if (isStringNullOrEmpty(openstackConfig.getRouter())) {
                LOG.error("-" + shortParam + " option is required!");
                return false;
            }
        }
        return true;
    }

    private boolean parseNetworkParameter() {
        final String shortParam = RuleBuilder.RuleNames.NETWORK_S.toString();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                openstackConfig.setNetwork(value);
            }
        }
        // Validate parameter if required
        if (req.contains(shortParam)) {
            if (isStringNullOrEmpty(openstackConfig.getNetwork())) {
                LOG.error("-" + shortParam + " option is required!");
                return false;
            }
        }
        return true;
    }

    private boolean parseSecurityGroupParameter() {
        final String shortParam = RuleBuilder.RuleNames.SECURITY_GROUP_S.toString();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                openstackConfig.setSecurityGroup(value);
            }
        }
        // Validate parameter if required
        if (req.contains(shortParam)) {
            if (isStringNullOrEmpty(openstackConfig.getSecurityGroup())) {
                LOG.error("-" + shortParam + " option is required!");
                return false;
            }
        }
        return true;
    }
}
