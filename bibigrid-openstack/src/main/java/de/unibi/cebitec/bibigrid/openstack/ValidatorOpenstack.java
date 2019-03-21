package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.Validator;
import de.unibi.cebitec.bibigrid.core.model.IntentMode;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.util.ConfigurationFile;
import org.apache.commons.cli.CommandLine;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Openstack specfic implementation for a CommandValidatorOpenstack
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de, t.dilger(at)uni-bielefeld.de, jkrueger(at)cebitec.uni-bielefeld.de
 */
public final class ValidatorOpenstack extends Validator {
    private final ConfigurationOpenstack openstackConfig;

    ValidatorOpenstack(final CommandLine cl, final ConfigurationFile configurationFile,
                       final IntentMode intentMode, final ProviderModule providerModule)
            throws ConfigurationException {
        super(cl, configurationFile, intentMode, providerModule);
        openstackConfig = (ConfigurationOpenstack) config;
    }

    @Override
    protected Class<ConfigurationOpenstack> getProviderConfigurationClass() {
        return ConfigurationOpenstack.class;
    }

    @Override
    protected List<String> getRequiredOptions() {
        List<String> options = new ArrayList<>();

        switch (intentMode) {
            default:
                return null;
            case LIST:
                //options.add(RuleBuilder.RuleNames.REGION.getShortParam());
                break;
            case TERMINATE:
                options.add(IntentMode.TERMINATE.getShortParam());
                //options.add(RuleBuilder.RuleNames.REGION.getShortParam());
                break;
            case PREPARE:
            case CREATE:
//                options.add(RuleBuilder.RuleNames.SSH_USER.getShortParam());
//                options.add(RuleBuilder.RuleNames.USE_MASTER_AS_COMPUTE.getShortParam());
//                options.add(RuleBuilder.RuleNames.SLAVE_INSTANCE_COUNT.getShortParam());
//                options.add(RuleBuilder.RuleNames.MASTER_INSTANCE_TYPE.getShortParam());
//                options.add(RuleBuilder.RuleNames.MASTER_IMAGE.getShortParam());
//                options.add(RuleBuilder.RuleNames.SLAVE_INSTANCE_TYPE.getShortParam());
//                options.add(RuleBuilder.RuleNames.SLAVE_IMAGE.getShortParam());
//                options.add(RuleBuilder.RuleNames.KEYPAIR.getShortParam());
//                options.add(RuleBuilder.RuleNames.SSH_PRIVATE_KEY_FILE.getShortParam());
//                options.add(RuleBuilder.RuleNames.REGION.getShortParam());
//                options.add(RuleBuilder.RuleNames.AVAILABILITY_ZONE.getShortParam());
            case VALIDATE:
                break;
            case CLOUD9:
                options.add(IntentMode.CLOUD9.getShortParam());
//                options.add(RuleBuilder.RuleNames.SSH_USER.getShortParam());
//                options.add(RuleBuilder.RuleNames.KEYPAIR.getShortParam());
//                options.add(RuleBuilder.RuleNames.SSH_PRIVATE_KEY_FILE.getShortParam());
                break;
        }
        return options;
    }

    @Override
    protected boolean validateProviderParameters() {
        return loadAndparseCredentialParameters();

    }

    private boolean loadAndparseCredentialParameters() {
        if (config.getCredentialsFile() != null) {
            try {
                File credentialsFile = Paths.get(config.getCredentialsFile()).toFile();
                openstackConfig.setOpenstackCredentials(
                        new Yaml().loadAs(new FileInputStream(credentialsFile), OpenStackCredentials.class));
                OpenStackCredentials openStackCredentials = openstackConfig.getOpenstackCredentials();
                if (openStackCredentials.getDomain() == null) {
                    LOG.error("OpenstackCredentials property 'domain' not set!");
                    return false;
                }
                if (openStackCredentials.getTenantDomain() == null) {
                    LOG.error("OpenstackCredentials property 'tenantDomain' not set!");
                    return false;
                }
                if (openStackCredentials.getTenantName() == null) {
                    LOG.error("OpenstackCredentials property 'tenantName' not set!");
                    return false;
                }
                if (openStackCredentials.getEndpoint() == null) {
                    LOG.error("OpenstackCredentials property 'endpoint' not set!");
                    return false;
                }
                if (openStackCredentials.getUsername() == null) {
                    LOG.error("OpenstackCredentials property 'username' not set!");
                    return false;
                }
                if (openStackCredentials.getPassword() == null) {
                    LOG.error("OpenstackCredentials property 'password' not set!");
                    return false;
                }
                return true;


            } catch (FileNotFoundException e) {
                LOG.error("Failed to load openstack credentials file. {}", e);
            } catch (YAMLException e) {
                LOG.error("Failed to parse openstack credentials file. {}",
                        e.getCause() != null ? e.getCause().getMessage() : e);
            }
        } else {
            LOG.error("Option 'credentialsFile' not set");
        }
        return false;
    }

}