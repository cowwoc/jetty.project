//
// ========================================================================
// Copyright (c) 1995 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v. 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
// which is available at https://www.apache.org/licenses/LICENSE-2.0.
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package org.eclipse.jetty.test.client.transport;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.Destination;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http3.api.Session;
import org.eclipse.jetty.http3.api.Stream;
import org.eclipse.jetty.http3.client.HTTP3Client;
import org.eclipse.jetty.http3.client.transport.HttpClientTransportOverHTTP3;
import org.eclipse.jetty.http3.frames.HeadersFrame;
import org.eclipse.jetty.http3.server.HTTP3ServerConnectionFactory;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.io.TransportProtocol;
import org.eclipse.jetty.quic.client.ClientQuicConfiguration;
import org.eclipse.jetty.quic.client.QuicTransportProtocol;
import org.eclipse.jetty.quic.server.QuicServerConnectionFactory;
import org.eclipse.jetty.quic.server.QuicServerConnector;
import org.eclipse.jetty.quic.server.ServerQuicConfiguration;
import org.eclipse.jetty.server.MemoryConnector;
import org.eclipse.jetty.server.MemoryTransportProtocol;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.toolchain.test.MavenPaths;
import org.eclipse.jetty.toolchain.test.jupiter.WorkDir;
import org.eclipse.jetty.toolchain.test.jupiter.WorkDirExtension;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@ExtendWith(WorkDirExtension.class)
public class HTTP3TransportProtocolTest
{
    private SslContextFactory.Server sslServer;
    private Path pemServerDir;
    private Server server;
    private SslContextFactory.Client sslClient;
    private HttpClient httpClient;
    private HTTP3Client http3Client;

    @BeforeEach
    public void prepare(WorkDir workDir) throws Exception
    {
        sslServer = new SslContextFactory.Server();
        sslServer.setKeyStorePath(MavenPaths.findTestResourceFile("keystore.p12").toString());
        sslServer.setKeyStorePassword("storepwd");
        pemServerDir = workDir.getEmptyPathDir().resolve("server");
        Files.createDirectories(pemServerDir);

        QueuedThreadPool serverThreads = new QueuedThreadPool();
        serverThreads.setName("server");
        server = new Server(serverThreads);

        sslClient = new SslContextFactory.Client(true);

        ClientQuicConfiguration quicConfiguration = new ClientQuicConfiguration(sslClient, null);
        ClientConnector clientConnector = new ClientConnector();
        QueuedThreadPool clientThreads = new QueuedThreadPool();
        serverThreads.setName("client");
        clientConnector.setExecutor(clientThreads);
        clientConnector.setSelectors(1);
        http3Client = new HTTP3Client(quicConfiguration, clientConnector);
        httpClient = new HttpClient(new HttpClientTransportOverHTTP3(http3Client));
        server.addBean(httpClient);
    }

    @AfterEach
    public void dispose()
    {
        LifeCycle.stop(server);
    }

    @Test
    public void testDefaultTransportProtocol() throws Exception
    {
        ServerQuicConfiguration serverQuicConfig = new ServerQuicConfiguration(sslServer, pemServerDir);
        QuicServerConnector connector = new QuicServerConnector(server, serverQuicConfig, new HTTP3ServerConnectionFactory(serverQuicConfig));
        server.addConnector(connector);
        server.setHandler(new EmptyServerHandler());
        server.start();

        ContentResponse response = httpClient.newRequest("localhost", connector.getLocalPort())
            .timeout(5, TimeUnit.SECONDS)
            .send();

        assertThat(response.getStatus(), is(HttpStatus.OK_200));

        List<Destination> destinations = httpClient.getDestinations();
        assertThat(destinations.size(), is(1));
        Destination destination = destinations.get(0);
        TransportProtocol transportProtocol = destination.getOrigin().getTransportProtocol();
        if (transportProtocol instanceof TransportProtocol.Wrapper wrapper)
            transportProtocol = wrapper.unwrap();
        assertThat(transportProtocol, sameInstance(TransportProtocol.UDP_IP));

        HttpClientTransportOverHTTP3 httpClientTransport = (HttpClientTransportOverHTTP3)httpClient.getTransport();
        int networkConnections = httpClientTransport.getHTTP3Client().getClientConnector().getSelectorManager().getTotalKeys();
        assertThat(networkConnections, is(1));
    }

    @Test
    public void testExplicitTransportProtocol() throws Exception
    {
        ServerQuicConfiguration serverQuicConfig = new ServerQuicConfiguration(sslServer, pemServerDir);
        QuicServerConnector connector = new QuicServerConnector(server, serverQuicConfig, new HTTP3ServerConnectionFactory(serverQuicConfig));
        server.addConnector(connector);
        server.setHandler(new EmptyServerHandler());
        server.start();

        ContentResponse response = httpClient.newRequest("localhost", connector.getLocalPort())
            .transportProtocol(new QuicTransportProtocol(http3Client.getQuicConfiguration()))
            .timeout(5, TimeUnit.SECONDS)
            .send();

        assertThat(response.getStatus(), is(HttpStatus.OK_200));
    }

    @Test
    public void testMemoryTransportProtocol() throws Exception
    {
        ServerQuicConfiguration quicConfiguration = new ServerQuicConfiguration(sslServer, pemServerDir);
        QuicServerConnectionFactory quic = new QuicServerConnectionFactory(quicConfiguration);
        HTTP3ServerConnectionFactory h3 = new HTTP3ServerConnectionFactory(quic.getQuicConfiguration());
        MemoryConnector connector = new MemoryConnector(server, quic, h3);
        server.addConnector(connector);
        server.setHandler(new EmptyServerHandler());
        server.start();

        ContentResponse response = httpClient.newRequest("http://localhost/")
            .transportProtocol(new QuicTransportProtocol(new MemoryTransportProtocol(connector), http3Client.getQuicConfiguration()))
            .timeout(5, TimeUnit.SECONDS)
            .send();

        assertThat(response.getStatus(), is(HttpStatus.OK_200));

        HttpClientTransportOverHTTP3 httpClientTransport = (HttpClientTransportOverHTTP3)httpClient.getTransport();
        int networkConnections = httpClientTransport.getHTTP3Client().getClientConnector().getSelectorManager().getTotalKeys();
        assertThat(networkConnections, is(0));
    }

    @Test
    public void testUnixDomainTransportProtocol()
    {
        noUnixDomainForDatagramChannel();
    }

    @Test
    public void testLowLevelH3OverUDPIP() throws Exception
    {
        ServerQuicConfiguration serverQuicConfig = new ServerQuicConfiguration(sslServer, pemServerDir);
        QuicServerConnector connector = new QuicServerConnector(server, serverQuicConfig, new HTTP3ServerConnectionFactory(serverQuicConfig));
        server.addConnector(connector);
        server.setHandler(new EmptyServerHandler());
        server.start();

        InetSocketAddress socketAddress = new InetSocketAddress("localhost", connector.getLocalPort());
        Session.Client session = http3Client.connect(socketAddress, new Session.Client.Listener() {}).get(5, TimeUnit.SECONDS);

        CountDownLatch responseLatch = new CountDownLatch(1);
        MetaData.Request request = new MetaData.Request("GET", HttpURI.from("http://localhost/"), HttpVersion.HTTP_3, HttpFields.EMPTY);
        session.newRequest(new HeadersFrame(request, true), new Stream.Client.Listener()
        {
            @Override
            public void onResponse(Stream.Client stream, HeadersFrame frame)
            {
                MetaData.Response response = (MetaData.Response)frame.getMetaData();
                assertThat(response.getStatus(), is(HttpStatus.OK_200));
                responseLatch.countDown();
            }
        });

        assertTrue(responseLatch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testLowLevelH3OverMemory() throws Exception
    {
        ServerQuicConfiguration serverQuicConfig = new ServerQuicConfiguration(sslServer, pemServerDir);
        QuicServerConnectionFactory quic = new QuicServerConnectionFactory(serverQuicConfig);
        HTTP3ServerConnectionFactory h3 = new HTTP3ServerConnectionFactory(quic.getQuicConfiguration());
        MemoryConnector connector = new MemoryConnector(server, quic, h3);
        server.addConnector(connector);
        server.setHandler(new EmptyServerHandler());
        server.start();

        TransportProtocol transportProtocol = new QuicTransportProtocol(new MemoryTransportProtocol(connector), http3Client.getQuicConfiguration());
        Session.Client session = http3Client.connect(transportProtocol, connector.getLocalSocketAddress(), new Session.Client.Listener() {}, null).get(5, TimeUnit.SECONDS);

        CountDownLatch responseLatch = new CountDownLatch(1);
        MetaData.Request request = new MetaData.Request("GET", HttpURI.from("http://localhost/"), HttpVersion.HTTP_3, HttpFields.EMPTY);
        session.newRequest(new HeadersFrame(request, true), new Stream.Client.Listener()
        {
            @Override
            public void onResponse(Stream.Client stream, HeadersFrame frame)
            {
                MetaData.Response response = (MetaData.Response)frame.getMetaData();
                assertThat(response.getStatus(), is(HttpStatus.OK_200));
                responseLatch.countDown();
            }
        });

        assertTrue(responseLatch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testLowLevelH3OverUnixDomain()
    {
        noUnixDomainForDatagramChannel();
    }

    private static void noUnixDomainForDatagramChannel()
    {
        assumeTrue(false, "DatagramChannel over Unix-Domain is not supported yet by Java");
    }
}
