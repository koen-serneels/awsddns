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

package be.error.awsddns.actuator;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.route53.AmazonRoute53AsyncClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class AwsConnectionHealthIndicator implements HealthIndicator {

    @Value("${awsddns.aws.region}")
    private String awsRegion;
    @Value("${awsddns.aws.accesskey}")
    private String awsAccessKey;
    @Value("${awsddns.aws.secretkey}")
    private String awsSecretKey;

    @Override
    public Health health() {
        try {
            AmazonRoute53AsyncClientBuilder amazonRoute53AsyncClientBuilder = AmazonRoute53AsyncClientBuilder.standard();
            amazonRoute53AsyncClientBuilder.setRegion(awsRegion);
            amazonRoute53AsyncClientBuilder.setCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsAccessKey, awsSecretKey)));
            amazonRoute53AsyncClientBuilder.build().listHostedZones();
        } catch (RuntimeException runtimeException) {
            Health.down(runtimeException);
        }
        return Health.up().build();
    }

    @Override
    public Health getHealth(boolean includeDetails) {
        return HealthIndicator.super.getHealth(includeDetails);
    }
}
