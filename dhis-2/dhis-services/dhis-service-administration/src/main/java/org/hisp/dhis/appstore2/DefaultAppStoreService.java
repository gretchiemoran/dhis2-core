package org.hisp.dhis.appstore2;

/*
 * Copyright (c) 2004-2018, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.appmanager.AppStatus;
import org.hisp.dhis.setting.SettingKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hisp.dhis.commons.util.TextUtils.versionNumber;

/**
 * Created by zubair@dhis2.org on 07.09.17.
 */
public class DefaultAppStoreService implements AppStoreService
{
    private static final Log log = LogFactory.getLog( DefaultAppStoreService.class );

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AppManager appManager;

    @Override
    public List<WebApp> getAppStore()
    {
        WebApp[] apps = restTemplate.getForObject( SettingKey.APP_STORE_API_URL.getDefaultValue().toString(), WebApp[].class );

        return Arrays.asList( apps );
    }

    @Override
    public AppStatus installAppFromAppStore( String id )
    {
        if ( id == null )
        {
            return AppStatus.NOT_FOUND;
        }

        try
        {
            Optional<WebApp> webApp = getWebApp( id );

            if ( webApp.isPresent() )
            {
                WebApp app = webApp.get();

                URL url = new URL( app.getVersion( id ).getDownloadUrl() );

                return appManager.installApp( getFile( url ), app.getName() );
            }

            log.info( String.format( "No version found for id %s", id ) );

            return AppStatus.NOT_FOUND;
        }
        catch ( IOException ex )
        {
            throw new RuntimeException( "Failed to install app", ex );
        }
    }

    public String getNewestCompatibleVersion( String appName, String dhisVersion )
        throws IOException, NoSuchElementException
    {
        List<WebApp> webApps = getAppStore().stream()
            .filter( app -> app.getName().equals( appName ) )
            .collect( Collectors.toList() );

        if ( webApps.isEmpty() )
        {
            throw new NoSuchElementException( String.format( "Could not find any apps named '%s'", appName ) );
        }

        return webApps.get( 0 ).getNewestCompatibleVersion( dhisVersion ).getId();
    }

    public void updateAppStatus( Set<App> apps )
    {
        Map<String, WebApp> webApps = getAppStore().stream()
            .collect( Collectors.toMap( WebApp::getName, Function.identity() ) );

        apps.stream()
            .filter( app -> webApps.containsKey( app.getName() ) )
            .filter( app -> versionNumber( webApps.get( app.getName() ).getNewestVersion().getVersion() )
                > versionNumber( app.getVersion() ) )
            .forEach( app -> app.setAppStatus( AppStatus.OUT_OF_DATE ) );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private Optional<WebApp> getWebApp( String id )
        throws IOException
    {
        for ( WebApp app : getAppStore() )
        {
            for ( AppVersion version : app.getVersions() )
            {
                if ( id.equals( version.getId() ) )
                {
                    return Optional.of( app );
                }
            }
        }

        return Optional.empty();
    }

    private static File getFile( URL url )
        throws IOException
    {
        URLConnection connection = url.openConnection();

        BufferedInputStream in = new BufferedInputStream( connection.getInputStream() );

        File tempFile = File.createTempFile( "dhis", null );

        tempFile.deleteOnExit();

        FileOutputStream out = new FileOutputStream( tempFile );

        IOUtils.copy( in, out );

        return tempFile;
    }
}
