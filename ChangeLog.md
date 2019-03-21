Version 2.0.2 <- 2.0.1 (03/14/2019)

Features
- Monitoring: Replace Ganglia with Zabbix (#90) 

Fixes
- Ubuntu 16.04 Ganglia fails after master reboot (#112) 
- Update or remove support for Apache Mesos (#92) 
- Code cleanup #109 
- (Openstack) Using image "names" instead of "ids" causes a runtime exception #106 
- Add available memory to slurm config #105


Version 2.0.1 <- 2.0 (02/22/2019)

- Remove native support for cassandra and mesos (#92,#93)
- support server groups (#89)
- tag each single ansible tag for easier debugging (#86)
- add support for Ubuntu 18.04 (partly #85)
- fix bug concerning mounting volumes when using config-drives (#84)