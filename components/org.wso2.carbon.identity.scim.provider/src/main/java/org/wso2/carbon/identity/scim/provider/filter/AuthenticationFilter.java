/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.scim.provider.filter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationManagementUtil;
import org.wso2.carbon.identity.scim.provider.auth.SCIMAuthenticationHandler;
import org.wso2.carbon.identity.scim.provider.auth.SCIMAuthenticatorRegistry;
import org.wso2.carbon.identity.scim.provider.util.JAXRSResponseBuilder;
import org.wso2.charon.core.encoder.json.JSONEncoder;
import org.wso2.charon.core.exceptions.UnauthorizedException;
import org.wso2.charon.core.protocol.endpoints.AbstractResourceEndpoint;

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

public class AuthenticationFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Log log = LogFactory.getLog(AuthenticationFilter.class);

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        // reset anything set on provisioning thread local.
        IdentityApplicationManagementUtil.resetThreadLocalProvisioningServiceProvider();

        if (log.isDebugEnabled()) {
            log.debug("Authenticating SCIM request..");
        }
        SCIMAuthenticatorRegistry SCIMAuthRegistry = SCIMAuthenticatorRegistry.getInstance();
        if (SCIMAuthRegistry != null) {
            SCIMAuthenticationHandler SCIMAuthHandler = SCIMAuthRegistry.getAuthenticator(containerRequestContext);
            boolean isAuthenticated = false;
            if (SCIMAuthHandler != null) {
                isAuthenticated = SCIMAuthHandler.isAuthenticated(containerRequestContext);
                if (isAuthenticated) {
                    return;
                }
            }
        }
        //if null response is not returned(i.e:message continues its way to the resource), return error & terminate.
        UnauthorizedException unauthorizedException = new UnauthorizedException(
                "Authentication failed for this resource.");
        containerRequestContext.abortWith(new JAXRSResponseBuilder().buildResponse(
                AbstractResourceEndpoint.encodeSCIMException(new JSONEncoder(), unauthorizedException)));
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext,
                       ContainerResponseContext containerResponseContext) throws IOException {
        IdentityApplicationManagementUtil.resetThreadLocalProvisioningServiceProvider();
    }
}
