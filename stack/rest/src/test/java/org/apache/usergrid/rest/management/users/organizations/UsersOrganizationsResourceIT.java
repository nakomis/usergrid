package org.apache.usergrid.rest.management.users.organizations;


import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.rest.AbstractRestIT;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import org.junit.Test;

import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


/** @author zznate */
@Concurrent()
public class UsersOrganizationsResourceIT extends AbstractRestIT {


    @Test
    public void createOrgFromUserConnectionFail() throws Exception {


        Map<String, String> payload = hashMap( "email", "orgfromuserconn@apigee.com" ).map( "password", "password" )
                .map( "organization", "orgfromuserconn" );

        JsonNode node = mapper.valueToTree(resource().path( "/management/organizations" ).accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).post( HashMap.class, payload ));

        String userId = node.get( "data" ).get( "owner" ).get( "uuid" ).asText();

        assertNotNull( node );

        String token = mgmtToken( "orgfromuserconn@apigee.com", "password" );

        node = mapper.valueToTree(resource().path( String.format( "/management/users/%s/", userId ) ).queryParam( "access_token", token )
                .type( MediaType.APPLICATION_JSON_TYPE ).get( HashMap.class ));

        logNode( node );

        payload = hashMap( "organization", "Orgfromuserconn" );

        // try to create the same org again off the connection
        try {
            node = mapper.valueToTree(resource().path( String.format( "/management/users/%s/organizations", userId ) )
                    .queryParam( "access_token", token ).accept( MediaType.APPLICATION_JSON )
                    .type( MediaType.APPLICATION_JSON_TYPE ).post( HashMap.class, payload ));
            fail( "Should have thrown unique exception on org name" );
        }
        catch ( Exception ex ) {
        }
    }
}