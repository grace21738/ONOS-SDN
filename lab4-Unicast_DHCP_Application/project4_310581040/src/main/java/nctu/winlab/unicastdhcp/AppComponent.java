/*
 * Copyright 2023-present Open Networking Foundation
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
package nctu.winlab.unicastdhcp;

//import org.onosproject.cfg.ComponentConfigService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Properties;
import java.util.Set;
//import java.util.List;
import java.util.HashMap;
//import java.lang.Iterable;


//
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_ADDED;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_UPDATED;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;


import org.onlab.packet.IPv4;
//import org.onlab.packet.ARP;
import org.onlab.packet.DHCP;
import org.onlab.packet.UDP;
import org.onlab.packet.TpPort;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

//Packet
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketProcessor;

import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;

//Path
import org.onosproject.net.topology.PathService;
import org.onosproject.net.Path;
//import org.onosproject.net.Link;

//Intant
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.FilteredConnectPoint;

import static org.onlab.util.Tools.get;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true,
           service = {SomeInterface.class},
           property = {
               "someProperty=Some Default String Value",
           })
public class AppComponent implements SomeInterface {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final DhcpConfigListener cfgListener = new DhcpConfigListener();

    private final ConfigFactory<ApplicationId, DhcpConfig> factory = new ConfigFactory<ApplicationId, DhcpConfig>(
          APP_SUBJECT_FACTORY, DhcpConfig.class, "UnicastDhcpConfig") {
        @Override
        public DhcpConfig createConfig() {
          return new DhcpConfig();
        }
    };

    private ApplicationId appId;
    protected ConnectPoint dhcpCP;
    protected HashMap<MacAddress, ConnectPoint> macTable = new HashMap<>();

    private DhcpPacketProcessor processor = new DhcpPacketProcessor();

    /** Some configurable property. */
    private String someProperty;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PathService pathService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected IntentService intentService;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication("nctu.winlab.unicastdhcp");
        packetService.addProcessor(processor, PacketProcessor.director(1));
        //cfg service
        cfgService.addListener(cfgListener);
        cfgService.registerConfigFactory(factory);
        //Request packet in via packet service.
        requestPackets();
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        cfgService.removeListener(cfgListener);
        cfgService.unregisterConfigFactory(factory);
        packetService.removeProcessor(processor);
        removeIntent();
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context != null ? context.getProperties() : new Properties();
        if (context != null) {
            someProperty = get(properties, "someProperty");
        }
        log.info("Reconfigured");
    }

    @Override
    public void someMethod() {
        log.info("Invoked");
    }

    /**
     * Request packet in via PacketService.
     */
    //Packet-in
    private void requestPackets() {

        TrafficSelector.Builder selectorServer = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_UDP)
                .matchUdpDst(TpPort.tpPort(UDP.DHCP_SERVER_PORT))//67
                .matchUdpSrc(TpPort.tpPort(UDP.DHCP_CLIENT_PORT));
        packetService.requestPackets(selectorServer.build(), PacketPriority.CONTROL, appId);

        selectorServer = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_UDP)
                .matchUdpDst(TpPort.tpPort(UDP.DHCP_CLIENT_PORT))//67
                .matchUdpSrc(TpPort.tpPort(UDP.DHCP_SERVER_PORT));
        packetService.requestPackets(selectorServer.build(), PacketPriority.CONTROL, appId);
    }

    private void removeIntent() {
        Iterable<Intent> intents = intentService.getIntentsByAppId(appId);
        for (Intent tent: intents) {
            intentService.withdraw(tent);
            log.info("Remove Intent");
        }
    }

    //Get dhcp config
    private class DhcpConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            if ((event.type() == CONFIG_ADDED || event.type() == CONFIG_UPDATED)
                && event.configClass().equals(DhcpConfig.class)) {
                DhcpConfig config = cfgService.getConfig(appId, DhcpConfig.class);
                if (config != null) {
                    String dhcpLocation = config.serverLocation();
                    String[] dhcpInfo = dhcpLocation.split("/");
                    //Set DHCP config
                    DeviceId dhcpDevice = DeviceId.deviceId(dhcpInfo[0]);
                    PortNumber dhcpPort = PortNumber.portNumber(dhcpInfo[1]);
                    dhcpCP = new ConnectPoint(dhcpDevice, dhcpPort);
                    log.info("DHCP server is connected to `{}`, port `{}`", dhcpInfo[0], dhcpInfo[1]);

                }
            }
        }
    }

    //Processor
    private class DhcpPacketProcessor implements PacketProcessor {
        @Override
        public void process(PacketContext context) {
            Ethernet packet = context.inPacket().parsed();

            if (context.isHandled()) {
                return;
            }

            if (packet.getEtherType() == Ethernet.TYPE_IPV4) {
                IPv4 ipv4Packet = (IPv4) packet.getPayload();

                if (ipv4Packet.getProtocol() == IPv4.PROTOCOL_UDP) {
                    UDP udpPacket = (UDP) ipv4Packet.getPayload();

                    if ((udpPacket.getDestinationPort() == UDP.DHCP_SERVER_PORT &&
                            udpPacket.getSourcePort() == UDP.DHCP_CLIENT_PORT) ||
                        (udpPacket.getDestinationPort() == UDP.DHCP_CLIENT_PORT &&
                            udpPacket.getSourcePort() == UDP.DHCP_SERVER_PORT)) {
                        // This is meant for the dhcp server so process the packet here.
                        DHCP dhcpPayload = (DHCP) udpPacket.getPayload();
                        processDhcpPacket(context, dhcpPayload);
                    }
                }
            }
        }


        private void processDhcpPacket(PacketContext context, DHCP dhcpPacket) {

            InboundPacket   inPacket    = context.inPacket();
            Ethernet        packet      = inPacket.parsed();
            ConnectPoint    cp          = inPacket.receivedFrom();
            //DHCP.MsgType incomingPacketType = null;
            if (dhcpPacket.getPacketType() == DHCP.MsgType.DHCPDISCOVER ||
                dhcpPacket.getPacketType() == DHCP.MsgType.DHCPREQUEST) {
                MacAddress      srcMac = packet.getSourceMAC();
                macTable.putIfAbsent(srcMac, cp);
                setPathRule(context, cp, dhcpCP);
                return;
            }

            if (dhcpPacket.getPacketType() == DHCP.MsgType.DHCPOFFER ||
                dhcpPacket.getPacketType() == DHCP.MsgType.DHCPACK) {
                MacAddress      dstMac  = packet.getDestinationMAC();
                ConnectPoint    dstCP   = macTable.get(dstMac);
                setPathRule(context, cp, dstCP);
                return;
            }

        }

        private void setPathRule(PacketContext context, ConnectPoint srcCP, ConnectPoint dstCP) {
            InboundPacket   inPacket    = context.inPacket();
            Ethernet        macPkt      = inPacket.parsed();
            ConnectPoint    cp          = inPacket.receivedFrom();

            PortNumber toServerPort = dstCP.port();

            if (srcCP.deviceId().toString().equals(dstCP.deviceId().toString())) {
                //log.info("[LAST] === [SRC] device {}, [DST] device {} ===",
                //     srcCP.deviceId().toString(), dstCP.deviceId().toString());
                context.treatmentBuilder().setOutput(dstCP.port());
                context.send();
                return;
            }
            //log.info("===[SRC] device {}, [DST] device {}===",
            //        srcCP.deviceId().toString(), dstCP.deviceId().toString());
            Set<Path> paths = pathService.getPaths(srcCP.deviceId(), dstCP.deviceId());
            // p.links() => list ||| for( link : p.links())
            for (Path path: paths) {
                if (path.links() != null && !path.links().isEmpty()) {
                    //Install instant Flow Rule
                    toServerPort = path.links().get(0).src().port();
                    /*
                    for (Link link: path.links()) {
                        log.info("[SRC] device {}, port {}, [DST] device {}, port {}",
                                link.src().deviceId().toString(), link.src().port().toString(),
                                link.dst().deviceId().toString(), link.dst().port().toString());
                    }
                    */
                    //log.info("Set output port {}", toServerPort.toString());
                    // Send to next switch
                    context.treatmentBuilder().setOutput(toServerPort);
                    context.send();
                    generateIntents(macPkt, toServerPort, srcCP, dstCP);
                    break;
                }
            }
            return;

        }

        private void generateIntents(Ethernet macPkt, PortNumber toServerPort, ConnectPoint ingress,
                                     ConnectPoint egress) {
            //Match
            TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
            TrafficSelector selector = selectorBuilder.matchEthDst(macPkt.getDestinationMAC()).build();
            if (egress == dhcpCP) {
                selector = selectorBuilder.matchEthDst(macPkt.getDestinationMAC()).build();
            }
            //Action
            TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
            TrafficTreatment treatment = treatmentBuilder.setOutput(toServerPort).build();

            PointToPointIntent intent = PointToPointIntent.builder()
                                          .appId(appId)
                                          .selector(selector)
                                          .treatment(treatment)
                                          .filteredIngressPoint(new FilteredConnectPoint(ingress))
                                          .filteredEgressPoint(new FilteredConnectPoint(egress))
                                          .build();
            log.info("Intent `{}`, port `{}` => `{}`, port `{}` is submitted.",
                     ingress.deviceId(), ingress.port(), egress.deviceId(), egress.port());
            intentService.submit(intent);

            /*Check Intent submit
            Iterable<Intent> intents= intentService.getIntentsByAppId(appId);
            for (Intent tent: intents){
                log.info("Intent");
            }*/
        }
    }

}
/*
private List<Intent> generateIntents(ConnectPoint ingress, ConnectPoint egress) {
  TrafficSelector.Builder selectorBldr = DefaultTrafficSelector.builder()
      .matchEthType(Ethernet.TYPE_IPV4);
  TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();
  List<Intent> intents = Lists.newArrayList();
  for (long i = 0; i < count; i++) {
    TrafficSelector selector = selectorBldr
        .matchEthSrc(MacAddress.valueOf(i + keyOffset))
        .build();
    intents.add(
        PointToPointIntent.builder()
          .appId(appId())
          .key(Key.of(i + keyOffset, appId()))
          .selector(selector)
          .treatment(treatment)
          .filteredIngressPoint(new FilteredConnectPoint(ingress))
          .filteredEgressPoint(new FilteredConnectPoint(egress))
          .build());
  }
  return intents;
}*/