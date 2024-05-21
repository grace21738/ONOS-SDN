/*
 * Copyright 2024-present Open Networking Foundation
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
package nycu.sdnfv.vrouter;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Dictionary;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;


//config
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_ADDED;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_UPDATED;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;

//Packet
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketContext;
//import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketProcessor;

import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;

import org.onlab.packet.IpPrefix;
import org.onlab.packet.IpAddress;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IPv4;
import org.onlab.packet.ARP;
//import org.onlab.packet.DHCP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;

import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceService;

//Host
import org.onosproject.net.host.HostService;

//Route
import org.onosproject.routeservice.RouteService;
import org.onosproject.routeservice.RouteInfo;
import org.onosproject.routeservice.RouteTableId;
import org.onosproject.routeservice.ResolvedRoute;

//Intant
//import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
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
    private final MyConfigListener cfgListener = new MyConfigListener();

    private final ConfigFactory<ApplicationId, MyConfig> factory = new ConfigFactory<ApplicationId, MyConfig>(
          APP_SUBJECT_FACTORY, MyConfig.class, "router") {
        @Override
        public MyConfig createConfig() {
          return new MyConfig();
        }
    };

    private ApplicationId appId;
    private RouterProcessor processor = new RouterProcessor();

    protected String sdnSubnet = "192.168.50";
    protected String othersSubnet = "192.168.5";
    protected ConnectPoint quaggaCP;
    protected List<IpAddress> peerIPs;
    protected ArrayList<IpAddress> quaggaIPs = new ArrayList<IpAddress>();
    protected HashMap<IpAddress, Set<IpAddress>> routeNetworks = new HashMap<>();

    protected MacAddress quaggaMac;
    protected IpAddress virtualIP;
    protected MacAddress virtualMac;
    protected Collection<RouteTableId> routeTablesid;


    /** Some configurable property. */
    private String someProperty;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected InterfaceService intfService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected RouteService routeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication("nycu.sdnfv.vrouter");
        packetService.addProcessor(processor, PacketProcessor.director(6));
        log.info("Using routeService to get route tables");

        //cfg service
        cfgService.addListener(cfgListener);
        cfgService.registerConfigFactory(factory);

        ConnectPoint    dstCP  = null;
        //Using Host Service host CP
        

        //Request packet in via packet service.
        packetService.requestPackets(
            DefaultTrafficSelector.builder().matchEthType(Ethernet.TYPE_IPV4).build(),
            PacketPriority.REACTIVE, appId
        );
        
        packetService.requestPackets(
            DefaultTrafficSelector.builder().matchEthType(Ethernet.TYPE_ARP).build(),
            PacketPriority.REACTIVE, appId
        );

        //Initial Quagga Array List
        quaggaIPs.add(IpAddress.valueOf("172.30.1.1"));
        quaggaIPs.add(IpAddress.valueOf("172.30.2.1"));
        quaggaIPs.add(IpAddress.valueOf("172.30.3.1"));
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        cfgService.removeListener(cfgListener);
        cfgService.unregisterConfigFactory(factory);
        //packetService.removeProcessor(processor);
        //removeIntent();
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

    protected void hostServiceDebug() {
            log.info("====IPv4 Host====");
            log.info("[Config Host]");
            Iterator<Host> hosts = hostService.getHosts().iterator();
            while( hosts.hasNext() ){
                Set<IpAddress> hostIPs = hosts.next().ipAddresses();
                for (IpAddress ip : hostIPs) {
                    log.info("[All Host ip]: `{}`", ip.toString());
                }
            }
            log.info("[Config Route]");
            Collection<RouteInfo> routeTables = routeService.getRoutes(new RouteTableId("ipv4"));
            for (RouteInfo routeTable : routeTables){
                Set<ResolvedRoute> routes = routeTable.allRoutes();
                for (ResolvedRoute route : routes ){
                    log.info("Route  ip `{}`, Route next hop `{}`", route.prefix().address().toString(),
                                                                    route.nextHop().toString());
                }
            }
            /*
            Collection<RouteTableId> routesTablesID = routeService.getRouteTables();
            for (RouteTableId id : routesTablesID){
                log.info("Route Table ID: `{}`", id.toString());
                Collection<RouteInfo> routeTables = routeService.getRoutes(new RouteTableId("ipv4"));
                for (RouteInfo routeTable : routeTables){
                    Set<ResolvedRoute> routes = routeTable.allRoutes();
                    for (ResolvedRoute route : routes ){
                        log.info("Route  ip `{}`, Route next hop `{}`", route.prefix().address().toString(),
                                                                        route.nextHop().toString());
                    }
                }
            }*/
        }

    protected void generateBGPIntents(IpAddress filterAddr, ConnectPoint ingress,
                                     ConnectPoint egress) {
        //Match
        //IpPrefix ipSelect = new IpPrefix.valueOf(filterAddr, 24);
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        TrafficSelector selector = selectorBuilder.matchEthType(Ethernet.TYPE_IPV4)
                                                  .matchIPDst(IpPrefix.valueOf(filterAddr, 24)).build();

        PointToPointIntent intent = PointToPointIntent.builder()
                                      .appId(appId)
                                      .selector(selector)
                                      //.treatment(treatment)
                                      .filteredIngressPoint(new FilteredConnectPoint(ingress))
                                      .filteredEgressPoint(new FilteredConnectPoint(egress))
                                      .build();
        
        //log.info("Intent `{}`, port `{}` => `{}`, port `{}` is submitted.",
        //         ingress.deviceId(), ingress.port(), egress.deviceId(), egress.port());
        intentService.submit(intent);
    }

    protected void generateChangeMACIntent(IpAddress filterAddr, ConnectPoint ingress,
                                     ConnectPoint egress, MacAddress srcMAC, MacAddress dstMAC) {
            //Match
            //IpPrefix ipSelect = new IpPrefix.valueOf(filterAddr, 24);
            TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
            TrafficSelector selector = selectorBuilder.matchEthType(Ethernet.TYPE_IPV4)
                                                      .matchIPDst(IpPrefix.valueOf(filterAddr, 24)).build();
            //Action
            TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
            TrafficTreatment treatment = treatmentBuilder.setEthSrc(srcMAC)
                                                         .setEthDst(dstMAC).build();

            PointToPointIntent intent = PointToPointIntent.builder()
                                          .appId(appId)
                                          .selector(selector)
                                          .treatment(treatment)
                                          .filteredIngressPoint(new FilteredConnectPoint(ingress))
                                          .filteredEgressPoint(new FilteredConnectPoint(egress))
                                          .build();
            
            log.info("[ChangeMACIntent] Intent `{}`, port `{}` => `{}`, port `{}` is submitted.",
                     ingress.deviceId(), ingress.port(), egress.deviceId(), egress.port());
            intentService.submit(intent);
        }

    protected void generateMultiPointIntent(IpAddress dstSubnet, Set<FilteredConnectPoint> ingresses,
                                     ConnectPoint egress, MacAddress dstMAC) {
        //Match
        //IpPrefix ipSelect = new IpPrefix.valueOf(filterAddr, 24);
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        TrafficSelector selector = selectorBuilder.matchEthType(Ethernet.TYPE_IPV4)
                                                  .matchIPDst(IpPrefix.valueOf(dstSubnet, 24)).build();
        //Action
        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
        TrafficTreatment treatment = treatmentBuilder.setEthSrc(quaggaMac)
                                                     .setEthDst(dstMAC).build();

        MultiPointToSinglePointIntent intent = MultiPointToSinglePointIntent.builder()
                                                .appId(appId)
                                                .selector(selector)
                                                .treatment(treatment)
                                                .filteredIngressPoints(ingresses)
                                                .filteredEgressPoint(new FilteredConnectPoint(egress))
                                                .build();
        
        //log.info("[Multi Intent] Egress `{}`, port `{}` is submitted.", egress.deviceId(), egress.port());
        intentService.submit(intent);
    }

    protected void generateDefaultIntents(){
        //Quagga -> External 
        for (int cnt = 0; cnt < quaggaIPs.size(); cnt ++) {
            Interface outIntf = intfService.getMatchingInterface(peerIPs.get(cnt));
            ConnectPoint eRouteCP = outIntf.connectPoint();
            //log.info("[outIntf] {}, port {}", eRouteCP.deviceId().toString(), eRouteCP.port().toString());

            generateBGPIntents(peerIPs.get(cnt), quaggaCP, eRouteCP);
            generateBGPIntents(quaggaIPs.get(cnt), eRouteCP, quaggaCP);
        }
        //External -> External
        // Set subNet from routeService
        for (IpAddress network : peerIPs) {
            Set<IpAddress> value = new HashSet<>(peerIPs);
            value.remove(network);
            routeNetworks.putIfAbsent(network, value);
        }

        for(IpAddress network : peerIPs) {
            Set<IpAddress> othersNetwork = routeNetworks.get(network);
            Set<FilteredConnectPoint> ingresses = new HashSet<>();
            //log.info("===Network  ip `{}`===", network.toString());
            //Ingresspoints
            for(IpAddress otherNetwork : othersNetwork) {
                //log.info("`{}`", otherNetwork.toString());
                Interface outIntf = intfService.getMatchingInterface(otherNetwork);
                if (outIntf != null) {
                    ConnectPoint otherCP = outIntf.connectPoint();
                    ingresses.add(new FilteredConnectPoint(otherCP));
                }
            }
            //Egresspoint
            Interface outIntf = intfService.getMatchingInterface(network);
            ConnectPoint egress = null;
            if (outIntf != null) {
                egress = outIntf.connectPoint();
                //log.info("[Interface Service] {}, port {}", packetOutCP.deviceId().toString(),
                //                                            packetOutCP.port().toString());
            }
            //Using Host Service get Next Hop MAC
            Set<Host> hostsByIP = hostService.getHostsByIp(network);
            for (Host host : hostsByIP) {
                MacAddress nexthopMAC = host.mac();
                if(!ingresses.isEmpty() && egress != null) {
                    generateMultiPointIntent(network, ingresses,
                                            egress, nexthopMAC);
                }
            }
        }
    }


    //Get dhcp config
    private class MyConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            if ((event.type() == CONFIG_ADDED || event.type() == CONFIG_UPDATED)
                && event.configClass().equals(MyConfig.class)) {
                MyConfig config = cfgService.getConfig(appId, MyConfig.class);

                if (config != null) {
                    //Set Quagga Connect Point
                    String quaggaConnect = config.quaggaConnect();
                    String[] quaggaInfo = quaggaConnect.split("/");
                    DeviceId quaggaDevice = DeviceId.deviceId(quaggaInfo[0]);
                    PortNumber quaggaPort = PortNumber.portNumber(quaggaInfo[1]);
                    quaggaCP = new ConnectPoint(quaggaDevice, quaggaPort);
                    log.info("quaggaCP is connected to `{}`, port `{}`", quaggaInfo[0], quaggaInfo[1]);

                    //Set Quagga Mac Address
                    //String quaggaMacStr = config.quaggaMac();
                    quaggaMac = MacAddress.valueOf(config.quaggaMac());
                    virtualIP = IpAddress.valueOf(config.virtualIP());
                    virtualMac = MacAddress.valueOf(config.virtualMac());
                    //log.info("quaggaConnect: `{}`", quaggaConnect);
                    log.info("quaggaMac: `{}`", quaggaMac);
                    log.info("virtualIP: `{}`", virtualIP);
                    log.info("virtualMac: `{}`", virtualMac);
                    peerIPs = new ArrayList<>(config.peerIP());
                    log.info("peerIP: `{}`, `{}`, `{}`", peerIPs.get(0).toString(), peerIPs.get(1).toString(),
                                                         peerIPs.get(2).toString());

                    //Quagga -> External 
                    generateDefaultIntents();
                }
            }
        }
    }

    //Processor
    private class RouterProcessor implements PacketProcessor {
        @Override
        public void process(PacketContext context) {
            Ethernet ethPkt = context.inPacket().parsed();

            if (context.isHandled()) {
                return;
            }

            if (ethPkt.getEtherType() == Ethernet.TYPE_IPV4) {
                //context.block();
                IPv4            ipv4Pkt =   (IPv4) ethPkt.getPayload();
                IpAddress       srcIP   =   IpAddress.valueOf(ipv4Pkt.getSourceAddress());
                IpAddress       dstIP   =   IpAddress.valueOf(ipv4Pkt.getDestinationAddress());
                ConnectPoint    packetinCP      =   context.inPacket().receivedFrom();

                MacAddress      srcMAC     =   ethPkt.getSourceMAC();
                MacAddress      dstMAC     =   ethPkt.getDestinationMAC();
                
                //Generate Default Intents
                //generateDefaultIntents();
                if ( !((srcIP.toString().contains(sdnSubnet) || srcIP.toString().contains(othersSubnet))
                    && (dstIP.toString().contains(sdnSubnet) || dstIP.toString().contains(othersSubnet)))) {
                    return;
                }
                generateDefaultIntents();
                /*
                if (!(srcIP.toString().contains("172.30") && dstIP.toString().contains("172.30"))) {
                    log.info("[PING IP] Source IP {}, Destination IP {}", srcIP.toString(), dstIP.toString());
                    log.info("[PING MAC] Source MAC {}, Destination MAC {}", srcMAC.toString(), dstMAC.toString());
                    hostServiceDebug();
                }*/

                ///Inter Domain
                //Outgoing BGP
                if (srcIP.toString().contains(sdnSubnet) && ! dstIP.toString().contains(sdnSubnet)) {
                    context.block();
                    //Get Route
                    log.info("**[Outgoing BGP]** SRC ip `{}` DST ip `{}`", srcIP.toString(), dstIP.toString());
                    Collection<ResolvedRoute> routes = routeService.getAllResolvedRoutes(IpPrefix.valueOf(dstIP, 24));
                    ConnectPoint    packetOutCP = null;
                    IpAddress       nexthopIP   = null;
                    MacAddress      nexthopMAC  = null;
                    for (ResolvedRoute route : routes) {
                        //log.info("[Route Service] Route IP: `{}`, Next Hop `{}`", route.prefix().address().toString(),
                        //                                          route.nextHop().toString());
                        nexthopIP = route.nextHop();
                        Interface outIntf = intfService.getMatchingInterface(nexthopIP);
                        if (outIntf != null) {
                            packetOutCP = outIntf.connectPoint();
                            //log.info("[Interface Service] {}, port {}", packetOutCP.deviceId().toString(),
                            //                                            packetOutCP.port().toString());
                        }

                        //Using Host Service get Next Hop MAC
                        Set<Host> hostsByIP = hostService.getHostsByIp(nexthopIP);
                        for (Host host : hostsByIP) {
                            nexthopMAC = host.mac();
                            //log.info("[Next Hop info] Next Hop IP: `{}`, MAC `{}`", nexthopIP.toString(),
                            //                                                        nexthopMAC.toString());
                        }
                    }
                    //hostServiceDebug();
                    if (packetOutCP != null &&  nexthopIP != null && nexthopMAC != null) {
                        generateChangeMACIntent(dstIP, packetinCP,
                                                packetOutCP, quaggaMac, nexthopMAC);
                    }        
                }

                //Incoming BGP
                if (dstIP.toString().contains(sdnSubnet) && ! srcIP.toString().contains(sdnSubnet)) {
                    context.block();
                    log.info("***[Incoming BGP]*** SRC ip `{}` DST ip `{}`", srcIP.toString(), dstIP.toString());
                    ConnectPoint    packetOutCP = null;
                    MacAddress      pingdstMAC  = null;

                    //Using Host Service get Dst CP
                    //hostServiceDebug();
                    Set<Host> hostsByIP = hostService.getHostsByIp(dstIP);
                    for (Host host : hostsByIP) {
                        packetOutCP     = host.location();
                        pingdstMAC      = host.mac();
                        //log.info("[Dst ConnectPoint] DST device `{}`, port `{}`", packetOutCP.deviceId().toString(), 
                        //                                                          packetOutCP.port().toString());
                    }

                    if (packetOutCP != null &&  pingdstMAC != null) {
                        generateChangeMACIntent(dstIP, packetinCP,
                                                packetOutCP, virtualMac, pingdstMAC);
                    }
                }
            }
        }
    }


}
