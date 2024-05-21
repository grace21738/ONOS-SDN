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
package nctu.winlab.bridge;

import org.onosproject.cfg.ComponentConfigService;
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

import static org.onlab.util.Tools.get;

//
//import java.util.Map;
//import com.google.common.collect.Maps;
//

import java.util.HashMap;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.flow.FlowRuleService;

import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketContext;
//import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketProcessor;

import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
//import org.onosproject.net.flow.FlowRule;
//import org.onosproject.net.flow.DefaultFlowRule;

import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;

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

    /** Some configurable property. */
    private String someProperty;

    //protected Map<DeviceId, Map<MacAddress, PortNumber>> macTable = Maps.newConcurrentMap();
    protected HashMap<DeviceId, HashMap<MacAddress, PortNumber>> macTable = new HashMap<>();
    private int timeout = 30;
    private int priority = 30;
    private ApplicationId appId;

    private LearningBridgeProcessor bridgeProcessor = new LearningBridgeProcessor();


    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowObjectiveService flowObjectiveService;

    @Activate
    protected void activate() {
        cfgService.registerProperties(getClass());
        appId = coreService.registerApplication("nctu.winlab.bridge");
        packetService.addProcessor(bridgeProcessor, PacketProcessor.director(2));

        //Request packet in via packet service.
        packetService.requestPackets(
            DefaultTrafficSelector.builder().matchEthType(Ethernet.TYPE_IPV4).build(),
            PacketPriority.REACTIVE, appId
        );
        packetService.requestPackets(
            DefaultTrafficSelector.builder().matchEthType(Ethernet.TYPE_ARP).build(),
            PacketPriority.REACTIVE, appId
        );
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        flowRuleService.removeFlowRulesById(appId);
        packetService.removeProcessor(bridgeProcessor);
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


    private class LearningBridgeProcessor implements PacketProcessor {
        @Override
        public void process(PacketContext context) {
            if (context.isHandled()) {
                return;
            }
            //Represents a data packet intercepted from an infrastructure device.
            //InboundPacket   pkt = context.inPacket();
            Ethernet        ethPkt = context.inPacket().parsed();
            if (ethPkt.getEtherType() != Ethernet.TYPE_IPV4 && ethPkt.getEtherType() != Ethernet.TYPE_ARP) {
                return;
            }

            //Get Connect point device and port (.deviceId(), .port())
            ConnectPoint    cp = context.inPacket().receivedFrom();
            //Add new Switch Table
            macTable.putIfAbsent(cp.deviceId(), new HashMap<>());

            goThroughPacket(context, ethPkt, cp);
        }

        public void goThroughPacket(PacketContext context, Ethernet ethPkt, ConnectPoint cp) {
            // Get Source Destination Mac Addr
            MacAddress      src = ethPkt.getSourceMAC();
            MacAddress      dst = ethPkt.getDestinationMAC();

            //Get switch MAC Table
            HashMap<MacAddress, PortNumber> swTable = macTable.get(cp.deviceId());
            PortNumber  outPort = swTable.get(dst);

            //If swTable key src Not Exist Add MAC addr and input port to swTable
            if (!swTable.containsKey(src)) {

                swTable.put(src, cp.port());
                log.info(
                    "Add an entry to the port table of `{}`. MAC address: `{}` => Port: `{}`",
                    cp.deviceId().toString(), src.toString(), swTable.get(src).toString()
                );
            }

            ////PACKET OUT
            //[MISS] Destination Port Not Exist -> FLOOD
            if (outPort == null) {

                outPort = PortNumber.FLOOD;
                log.info("MAC address `{}` is missed on `{}`. Flood the packet.",
                    dst.toString(), cp.deviceId().toString()
                );
                context.treatmentBuilder().setOutput(outPort);

                //Triggers the outbound packet to be sent.
                context.send();
                return;
            }

            //[HIT] Destination Port is exist
            context.treatmentBuilder().setOutput(outPort);
            context.send();

            //install Flow Rule -> install once
            installFlowRule(context, cp, src, dst, outPort);
            return;
        }


        public void installFlowRule(PacketContext context, ConnectPoint cp, MacAddress src,
                                    MacAddress dst, PortNumber outputPort) {
            //Match
            TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
            selectorBuilder.matchEthSrc(src).matchEthDst(dst);
            //Action
            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
            .setOutput(outputPort)
            .build();

            log.info("MAC {} is matched on {}! Install flow rule!", dst.toString(), cp.deviceId().toString());

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
            .withSelector(selectorBuilder.build())
            .withTreatment(treatment)
            .withPriority(priority)
            .withFlag(ForwardingObjective.Flag.VERSATILE)
            .fromApp(appId)
            .makeTemporary(timeout)
            .add();

            flowObjectiveService.forward(context.inPacket().receivedFrom().deviceId(),
                                     forwardingObjective);

            return;
        }

    }

}