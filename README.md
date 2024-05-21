# SDN-ONOS

### 環境

<img src="images\onos_mininet.png" alt="onos_mininet" style="zoom: 50%;" />

## LAB1

> ONOS and Mininet Installation

### 簡介

建立 ONOS 以及 mininet，熟悉 ONOS 指令，且使用 python script 建立 network。

建立 ONOS 

```bash
~$ cd $ONOS_ROOT
~/onos$ bazel run onos-local -- clean debug
        # option 'clean' to delete all previous running status
        # option 'debug' to enable remote debugging (port 5005)
```

進入 onos CLI

```bash
~/onos$ onos localhost
```

建立 mininet

> 127.0.0.1:6653 為 onos IP
>
> project1_part2_<studentID>.py 為建立 mininet 的 python

```bash
~$ sudo mn --custom=project1_part2_<studentID>.py \
--topo=topo_part2_<studentID> \
--controller=remote,ip=127.0.0.1:6653 \
--switch=ovs,protocols=OpenFlow14
```

## LAB2 

> OpenFlow Protocol Observation and Flow Rule Installation

### 簡介

根據 ONOS 官方提供的 spec ，透過 json 檔或者 GUI 中的 Restful API 中建立 flow rule。並利用 wireshark 檢查封包傳送狀況。

+ Mininet and Network Namespace

  <img src="images\archi.png" alt="archi" style="zoom: 50%;" />

  + Mininet 使用 network namespace 去仿照 network
  + OVS 在 root network 中執行
  + 每個 host 獨立運作在自己的 namespace 中
  + 利用 veth pair 連接不同 namespace 的 network

+ flow.json

```json
{
    "priority": 50000,
    "timeout": 0,
    "isPermanent": true, 
    //判斷條件
    "selector": {
        "criteria": [
            {
            "type": "IN_PORT",
            "port": 1
            }
        ]
    },
    //滿足條件則執行
    "treatment": {
        "instructions": [
            {
                "type": "OUTPUT",
                "port": 2
            }
        ]
    }
}

```

## LAB3

> ONOS Application Development: SDN-enabled Learning Bridge

### 簡介

當傳送封包時 ONOS 怎麼運作，並將其控制封包的指令打包成 Application 並建立在 ONOS 上運行。

<img src="C:\Users\a3023\Desktop\ONOS-SDN\images\onos_control.png" alt="onos_control" style="zoom:60%;" />

先設定 ONOS packet in 條件，當 ONOS 收到傳送中的封包時:

1. 使用 hash table 紀錄 Source MAC 以及 incoming port 和哪個 switch device
2. 檢查 Destination MAC 是否有紀錄至 hash table 上
3. 若無，則 packet out FLOOD (告訴 switch flood packet)， 若有則packet out 要傳送出去device 以及 port，並且在 switch 上 install flow rule 

## LAB4

> Unicast DHCP Application

模擬當 ONOS 收到 DHCP 封包時如何傳送，實作的部分全部是 unicast (沒有 broadcast)，透過 config file 獲得 DHCP server 連接位置( Connectpoint -> switch & port )，再利用 ONOS 上的 IntentService 使用`PointToPoint` intent install flow rule。

模擬情境:

1. 當 host 1 連接至 switch 上時會 broadcast `DHCP DISCOVER` 去尋找 DHCP 位置
2. 當 DHCP 收到 `DHCP DISCOVER` 緊接著傳送 `DHCP OFFER` ( 可能是 unicast or broadcast )
3. host 1  收到後，傳送 `DHCP REQUEST` (broadcast) 索要 IP
4. DHCP 收到後 unicast `DHCPACK`  至 host 1 
5. host 1 獲得專屬的 IP

## LAB 5

> Proxy ARP

 當 ONOS 收到 ARP packet 時如何處理封包。

<img src="images\arp.png" alt="arp" style="zoom:67%;" />

先設定 ONOS 會將 ARP packet packet in， 模擬情境:

1. host 1 寄送 ARP request
2. ONOS Proxy ARP App (需實作的部分)收到封包後，紀錄 source IP 和 MAC address 至 hash table 中
3. 檢查 ARP request 中的 target IP 和 MAC 是否存在在 hash table
4. 若有，直接 packet out 一個 ARP Reply 至 host 1， 若沒有 ONOS 則 FLOOD  ARP request
5. 當 ARP reply packet in 時，hash table 儲存 Source IP 和 MAC address。

## LAB 6

> Network Function Virtualization: Software Router and Containerization

### 先備知識

**Quagga is an open-source software that provides routing services**

+ Support common routing protocols: BGP, OSPF, RIP ...
+ Consists of a core daemon Zebra and separate routing protocol daemons

Routing Protocols (daemons) communicate their best routes to Zebra

**Zebra  計算找出最佳路徑且透過 netlink 更改 kernel routing table **

<img src="images\quagga.png" alt="arp" style="zoom:67%;" />

+ Zebra supports a FIB Push Interface (FPI) : 

  FPI 允許其他外部元件學習 forwarding information

+ Forwarding Plane Manager (FPM):

  接收 FIB ，解碼 FIB routes， 執行 forwarding plane

+ FIB pushing: 

  FPM 跟 zebra 建立 TCP 連線， Zebra push FIB 至 FPM  

#### 利用 docker 建立網域

使用 docker  建立 host container 以及 network bridge

<img src="images\docker_network.png" alt="arp" style="zoom:67%;" />

1. 下載 Quagga image， 以及  host container Dockerfile 下載 

2. 建立 router 和 host container

3. 建立 bridge，且連接 host 和 router

4. 建立 host default gateway

5. 設置 router container 中的 bgpd (ASN + network + neighbor)，且 restart container

   > 上述可用 docker-compose.yml 建立

6. 進入 router container 檢查在 zebra 中的 bgp

### 建立Topology

<img src="images\topology.png" alt="topology" style="zoom: 50%;" />

# Final 

繼承上面所有 lab 實作，根據以下 Network 建立 flowrule (開發 ONOS App)

<img src="images\final_topo.png" alt="topology" style="zoom: 67%;" />

+ 處理 eBGP (External Router <=> OVS <=> Quagga)
+ 不同 domain  之間傳送 packet， 需更改 L2 Source & Destination 
  + SDN to External
  + External to SDN
  + External to External
