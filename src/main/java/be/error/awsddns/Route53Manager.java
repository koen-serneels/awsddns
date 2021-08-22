/*
 * Copyright 2021 Koen Serneels
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package be.error.awsddns;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.route53.AmazonRoute53Async;
import com.amazonaws.services.route53.AmazonRoute53AsyncClientBuilder;
import com.amazonaws.services.route53.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import static org.apache.commons.lang3.StringUtils.abbreviate;
import static org.apache.commons.lang3.StringUtils.join;

@Service
public class Route53Manager {

    private static Logger LOG = LoggerFactory.getLogger(Route53Manager.class);

    @Value("${awsddns.aws.region}")
    private String awsRegion;
    @Value("${awsddns.aws.accesskey}")
    private String awsAccessKey;
    @Value("${awsddns.aws.secretkey}")
    private String awsSecretKey;
    @Value("${awsddns.server.hostname}")
    private String ddnsServerHostname;

    private String ddnsServerHostedZoneId;
    private AmazonRoute53Async amazonRoute53Async;

    @Autowired
    private ClientIpRetriever clientIpRetriever;

    @PostConstruct
    public void Route53Manager() {
        LOG.debug("Initializing " + Route53Manager.class.getSimpleName() + ". AWSRegion: " + awsRegion + ", AWSAccessKey (first char): " + abbreviate(awsAccessKey, 4) + ", AWSSecretKey (first char): " + abbreviate(awsSecretKey, 4) + ", AWSDDNS Hostname:" + ddnsServerHostname);
        AmazonRoute53AsyncClientBuilder amazonRoute53AsyncClientBuilder = AmazonRoute53AsyncClientBuilder.standard();
        amazonRoute53AsyncClientBuilder.setRegion(awsRegion);
        amazonRoute53AsyncClientBuilder.setCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsAccessKey, awsSecretKey)));
        amazonRoute53Async = amazonRoute53AsyncClientBuilder.build();
        ddnsServerHostedZoneId = getHostedZone(ddnsServerHostname).getId();

        LOG.debug("Updating server ip at startup");
        updateServerIp();

        LOG.debug("Initialized " + Route53Manager.class.getSimpleName() + ". Using AWS hosted zone id: " + ddnsServerHostedZoneId);

    }

    public void updateIpFromClient(String clientHostname, String clientIpAddress) {
        LOG.debug("Processing ip update from client...");
        try {
            String hostedZoneId = getHostedZone(clientHostname).getId();
            LOG.debug("Updating " + clientHostname + " to ip address " + clientIpAddress + " in hosted zone id " + hostedZoneId);
            ChangeResourceRecordSetsRequest changeResourceRecordSetsRequest = new ChangeResourceRecordSetsRequest().withHostedZoneId(hostedZoneId).withChangeBatch(
                    new ChangeBatch().withComment("DDNS client record - do not edit").withChanges(
                            new Change().withAction("UPSERT").withResourceRecordSet(
                                    new ResourceRecordSet().withName(clientHostname).withType("A").withTTL(60L)
                                            .withResourceRecords(new ResourceRecord().withValue(clientIpAddress)))));
            Future<ChangeResourceRecordSetsResult> future = amazonRoute53Async.changeResourceRecordSetsAsync(changeResourceRecordSetsRequest);
            future.get();
            LOG.debug("[DONE] Processing ip update from client");
        } catch (
                Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Scheduled(cron = "0 * * * *")
    public void updateServerIp() {
        LOG.debug("Running scheduled Server ip update...");
        try {
            String ipAddress = clientIpRetriever.getIp();

            LOG.debug("Using returned ip: " + ipAddress);
            LOG.debug("Updating hostname " + ddnsServerHostname + " in zone " + ddnsServerHostedZoneId + " to ip address " + ipAddress);
            ChangeResourceRecordSetsRequest changeResourceRecordSetsRequest = new ChangeResourceRecordSetsRequest().withHostedZoneId(ddnsServerHostedZoneId).withChangeBatch(
                    new ChangeBatch().withComment("DDNS record - do not edit").withChanges(
                            new Change().withAction("UPSERT").withResourceRecordSet(
                                    new ResourceRecordSet().withName(ddnsServerHostname).withType("A").withTTL(60L)
                                            .withResourceRecords(new ResourceRecord().withValue(ipAddress)))));

            Future<ChangeResourceRecordSetsResult> future = amazonRoute53Async.changeResourceRecordSetsAsync(changeResourceRecordSetsRequest);
            future.get();
            LOG.debug("[DONE] Running scheduled Server ip update");
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private HostedZone getHostedZone(String host) {
        LOG.debug("Searching hosted zone for host: " + host);
        List<String> hostedZones = new ArrayList<>();
        for (HostedZone hostedZone : amazonRoute53Async.listHostedZones().getHostedZones()) {
            if (hostedZone.getConfig().isPrivateZone()) {
                LOG.debug("Ignore hosted zone  " + hostedZone.getName() + " because private");
                continue;
            }
            if (host.toLowerCase().endsWith(hostedZone.getName().toLowerCase().substring(0, hostedZone.getName().length() - 1))) {
                LOG.debug("Found matching hosted zone:" + hostedZone.getName() + " with id:" + hostedZone.getId());
                return hostedZone;
            }
            LOG.debug("Hosted zone  " + hostedZone.getName() + " does not match");
            hostedZones.add(hostedZone.getName());
        }
        throw new IllegalStateException("Could not find corresponding public hosted zone for host " + host + ". Current public hosted zone's:" + join(hostedZones, ","));
    }
}