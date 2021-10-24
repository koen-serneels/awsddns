# Dynamic DNS endpoint for AWS (Route53)
### A Dynamic DNS endpoint over HTTP(S)
Accepts requests from DDNS clients and adds/updates hosts within Route53 managed zones with the specified IP. 

For example; you own domain xyz, this domain  is hosted on AWS (Route 53) and you want a host within that domain (myhost.xyz) to be automatically updated with a dynamic IP.   
That IP might belong to a system running in your home network environment using a ISP provided dynamic IP address, or any other system with a dynamic IP for that matter.  
In that case the system running on your home network can use a DDNS client to update the IP via AWSDDNS.  
The client can be anything ranging from a piece of software that is installed on the system, build in client on a router or other device.  
Currently AWSDDNS supports the NO-IP protocol which is a well supported protocol in most DDNS clients.

AWSDDNS is a standalone service offered via a Docker container. It has low system requirements and has no dependencies (does not use a database etc).  
It can be deployed on any system supporting Docker. To detect the external IP address it depends on a public 'what's my IP address' service.  
By default it uses the AWS check ip address, but any service  accessible over HTTP(S) returning an IP address can be configured.

### AWS Route53 permissions
In order for AWSDDNS to update your Route53 zone's, it needs programatic access and therefore requires an access and secret key.  
The safest option is to create a policy only allowing access to the hostedzone's you want to allow creating dynamic hosts in.

1. Enable programmatic access. This can be enabled via the AWS management console. See https://aws.amazon.com/premiumsupport/knowledge-center/create-access-key/  
The access and secret key (copy/paste it somewhere temporary) need to be configured in the configuration file which will be addressed
in the subsequent topics.

2. Next, the user for which the access key is generated needs permissions to manage Route53. Below is a snippet for a
Policy that allows just that

```
{
"Version": "2012-10-17",
"Statement": [
{
"Effect": "Allow",
"Action": "route53:ChangeResourceRecordSets",
"Resource": "arn:aws:route53:::hostedzone/*"
},
{
"Effect": "Allow",
"Action": "route53:TestDNSAnswer",
"Resource": "*"
},
{
"Effect": "Allow",
"Action": "route53:ListHostedZones",
"Resource": "*"
}
]
}
```

*Note: the policy above allows access to all hosted zone's. This is in the assumption there are multiple domains and a
client can use either one of them to add hosts.  
If you want to restrict the domains, then replace the hostedzone with a specific hostedzone id: hostedzone/hostedzoneid.  
If there are multiple hostedzone you want to allow, then copy/paste the first block for each hosted zone that is allowed to be used.*

To summarize:

1. Create a new AWS user or use an existing one
2. Enable programatic access for that user generating an access and secret key
3. Create a policy that allows route53 management, for example via the policy shown here

*Note: for adding the policy there are three options. The policy can be added directly 'inline' to the user, meaning
the policy cannot be re-used and is in fact 'private' for that user.  
Or it can be added as a global policy and then added to the user (meaning it can easily be used for other users as well) ,
or the global policy is not directly added to the user, but added  to a group instead to which the user is then added.  
For AWSDDNS this doens't matter: all three options yield the result in terms of access rights.*

### AWSDDNS Installation
AWSDDNS is installed on a system supporting Docker. It doesn't matter which system, as long as it  supports Docker, 
has Internet access and can receive requests from the Internet, it can run AWSDDNS.

There are two installation scenario's shown. The first is the installation on an arbitrary system, for example, AWSDDNS
could hosted on an ec2 while its client might be a home computer using DDNS via a built-in router feature.  
In this case it is adviced to place a reverse proxy in front of AWSDDNS. 

The second is via Synology. In this case Synology is both the server; hosting AWSDDNS via its Docker feature
and the client; using the hosted AWSDDNS via its DDNS client support. This setup might be interesting if you have a Synology 
system at home that you want to make accessible over the Internet with a host within your own domain.

Note: in both installation scenario's AWSDDNS is pulled from DockerHub. It's possible to build it yourself instead.   
For more information on howto build the Docker image see [link](#build-it-yourself).

####Arbitrary system
TODO

####Synology

###Build it yourself
Make sure Docker and Docker Compose is installed on your system. Clone this repository, then

* `cd awsddns`
* `./gradlew build`
  * This will produce the self executing JAR located in build/libs
* `docker-build ./`
  * This will take the produced JAR and wrap it in a container image.  
    When listing images (*docker images*) it will show up under the Repository/tag: errorbe/awsddns
* create the directory `mkdir ~/awsddns` and `mkdir ~/awsddns/logs`
* Add the application.properties (for contents see) to ~/awsddns
* Then run `docker-compose up` from the cloned awsddns root directory
* If `wget http://localhost:8080/actuator/health` points up its "UP" then life is good
  * Use the admin user/pass configured in the application.properties
