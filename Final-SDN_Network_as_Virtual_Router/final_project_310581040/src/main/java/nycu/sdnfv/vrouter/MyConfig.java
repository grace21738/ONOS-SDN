/*
 * Copyright 2020-present Open Networking Foundation
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

import org.onlab.packet.IpAddress;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;
import java.util.List;
//import java.util.ArrayList;
import java.util.function.Function;

public class MyConfig extends Config<ApplicationId> {

  public static final String QCP = "quagga";
  public static final String QMAC = "quagga-mac";
  public static final String VIRTUALIP = "virtual-ip";
  public static final String VIRTUALMAC = "virtual-mac";
  public static final String PEER = "peers";

  @Override
  public boolean isValid() {
    return hasFields(QCP, QMAC, VIRTUALIP, VIRTUALMAC, PEER);
  }

  public String quaggaConnect() {
    return get(QCP, null);
  }

  public String quaggaMac() {
    return get(QMAC, null);
  }

  public String virtualIP() {
    return get(VIRTUALIP, null);
  }

  public String virtualMac() {
    return get(VIRTUALMAC, null);
  }

  public List<IpAddress> peerIP() {
    Function<String, IpAddress> mystring = x -> IpAddress.valueOf(x);
    return getList(PEER, mystring);
  }
}
