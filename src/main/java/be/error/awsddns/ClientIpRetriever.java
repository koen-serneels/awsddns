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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class ClientIpRetriever {

    private static final InetAddressValidator inetAddressValidator = InetAddressValidator.getInstance();

    private static Logger LOG = LoggerFactory.getLogger(ClientIpRetriever.class);

    @Value("${awsddns.ip.check.url}")
    private String ipCheckUrl;

    @PostConstruct
    public void ClientIpRetriever() {
        LOG.debug("Initializing " + ClientIpRetriever.class.getSimpleName() + ". IP check URL:" + ipCheckUrl);
    }

    public String getIp() {
        try {
            LOG.debug("Getting current ip from: " + ipCheckUrl);
            HttpClient httpClient = HttpClient.newHttpClient();
            URI uri = URI.create(ipCheckUrl);
            HttpRequest request = HttpRequest.newBuilder(uri).GET().build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (!HttpStatus.resolve(response.statusCode()).is2xxSuccessful()) {
                throw new IllegalStateException("Public ip address (url: " + ipCheckUrl + ") not in 2xx range. Response: " + response.statusCode() + " - " + response.body());
            }

            String ipAddress = StringUtils.chomp(response.body());
            if (!inetAddressValidator.isValid(ipAddress)) {
                throw new IllegalArgumentException("Response was not a valid ip address (url: " + ipCheckUrl + ")  Response: " + response.body());
            }
            return ipAddress;
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
