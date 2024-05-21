# LAB02 - OpenFlow Protocol Observation and Flow Rule Installation

> 310581040 智能所 簡郁宸

## Part 1: Answer Questions

1. **How many OpenFlow headers with type “OFPT_FLOW_MOD” and command “OFPFC_ADD” are there  among all the packets?** 

   **A:  共有六種** distinct  type 為`OFPT_FLOW_MOD` 且 command 為 `OFPFC_ADD` 的 OpenFlow headers

   1. <img src="C:\Users\a3023\Desktop\SDN_proj\lab2\image\head_1.png" alt="head_1" style="zoom: 67%;" />

   2. <img src="C:\Users\a3023\Desktop\SDN_proj\lab2\image\head_2.png" alt="head_2" style="zoom: 67%;" />

   3. <img src="C:\Users\a3023\Desktop\SDN_proj\lab2\image\head_3.png" alt="head_3" style="zoom: 67%;" />

   4. <img src="C:\Users\a3023\Desktop\SDN_proj\lab2\image\head_4.png" alt="head_4" style="zoom:67%;" />

   5. <img src="C:\Users\a3023\Desktop\SDN_proj\lab2\image\head_5.png" alt="head_5" style="zoom:67%;" />

   6. <img src="C:\Users\a3023\Desktop\SDN_proj\lab2\image\head_6.png" alt="head_6" style="zoom:67%;" />

2. **What are the match fields and the corresponding actions in each “OFPT_FLOW_MOD” message?** 

3. **What are the Idle Timeout values for all flow rules on s1 in GUI?**

   | Match Fields                                                 | Actions             | Timeout Values |
   | ------------------------------------------------------------ | ------------------- | -------------- |
   | ETH_TYPE = ARP                                               | OUTPUT = CONTROLLER | 0              |
   | ETH_TYPE = 802.1 Link Layer Discovery Protocol (LLDP)        | OUTPUT = CONTROLLER | 0              |
   | ETH_TYPE = Unknown (0x8942) **(bddp)**                       | OUTPUT = CONTROLLER | 0              |
   | ETH_TYPE = IPv4                                              | OUTPUT = CONTROLLER | 0              |
   | IN_PORT=2, ETH_DST=5a:64:4c:3d:47:52, ETH_SRC=ce:f3:b6:14:9b:5c | OUTPUT = 1          | 10             |
   | IN_PORT=1, ETH_DST=ce:f3:b6:14:9b:5c, ETH_SRC=5a:64:4c:3d:47:52 | OUTPUT = 2          | 10             |

   

## Part 2: Install Flow Rules

> IN_PORT -> Ingress port  Numerical representation of incoming port, starting at 1. This may be a physical or switch-defined logical port.
>
> OUTPUT -> The Output action forwards a packet to a specified OpenFlow port (see 4.1). OpenFlow switches must support forwarding to physical ports, switch-defined logical ports and the required reserved ports (see 4.5).

**Topology**

```bash
sudo mn --controller=remote,127.0.0.1:6653 --switch=ovs,protocols=OpenFlow14
```

### Install one flow rule to forward ARP packets

+ Match Fields 

  + Ethernet type (ARP)  

+ Actions 

  + Forwarding ARP packets to all port in one instruction 

    <img src="C:\Users\a3023\AppData\Roaming\Typora\typora-user-images\image-20231004234906968.png" alt="image-20231004234906968" style="zoom:80%;" />

    

    

+ Take **screenshot** to verify the flow rules you installed

  ```bash
  mininet> h1 arping h2
  ```

  <img src="C:\Users\a3023\Desktop\SDN_proj\lab2\image\arping.png" alt="arping" style="zoom: 80%;" />



### Install two flow rules to forward IPv4 packets 

+ Match Fields 

  +  IPv4 destination address and other required dependencies

+ Actions  

  + Forwarding IPv4 packets to the right host

    <img src="C:\Users\a3023\AppData\Roaming\Typora\typora-user-images\image-20231004234948453.png" alt="image-20231004234948453" style="zoom:80%;" />

+ Take **screenshot** to verify the flow rules you installed

  ```bash
  mininet> h1 ping h2
  ```

  <img src="C:\Users\a3023\Desktop\SDN_proj\lab2\image\ping.png" alt="ping" style="zoom:80%;" />

  

## Part 3: Create Topology with Broadcast Storm

+ Steps: 

  + Create a topology that may cause a “Broadcast Storm”. 

    ```bash
    sudo mn --custom=topo_310581040.py --topo=topo_310581040 --controller=remote,127.0.0.1:6653 --switch=ovs,protocols=OpenFlow14
    ```

  + Install flow rules on switches. 

    ```bash
    curl -v -u onos:rocks  -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d @flows_s1-1_310581040.json 'http://localhost:8181/onos/v1/flows/of:0000000000000001'
    curl -v -u onos:rocks  -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d @flows_s1-2_310581040.json 'http://localhost:8181/onos/v1/flows/of:0000000000000001'
    
    curl -v -u onos:rocks -X POST --header 'Content-Type: application/json' --header 'Accept: application/json'  -d @flows_s2-1_310581040.json 'http://localhost:8181/onos/v1/flows/of:0000000000000002'
    curl -v -u onos:rocks -X POST --header 'Content-Type: application/json' --header 'Accept: application/json'  -d @flows_s2-2_310581040.json 'http://localhost:8181/onos/v1/flows/of:0000000000000002'
    curl -v -u onos:rocks -X POST --header 'Content-Type: application/json' --header 'Accept: application/json'  -d @flows_s2-3_310581040.json 'http://localhost:8181/onos/v1/flows/of:0000000000000002'
    
    curl -v -u onos:rocks -X POST --header 'Content-Type: application/json' --header 'Accept: application/json'  -d @flows_s3-1_310581040.json 'http://localhost:8181/onos/v1/flows/of:0000000000000003'
    curl -v -u onos:rocks -X POST --header 'Content-Type: application/json' --header 'Accept: application/json'  -d @flows_s3-2_310581040.json 'http://localhost:8181/onos/v1/flows/of:0000000000000003'
    ```

  + Send packets from one host to another host. 

    ```bash
    h1 ping h2
    ```

  + Observe link status of the network and the CPUs utilization of VM

+ Describe what you have observed and explain why the broadcast storm occurred

  + broadcast storm 設計方式

    + Topology

      設計三個 switch 並且有 其中 s1 和 s3 連接 h1 和 h2

      <img src="C:\Users\a3023\Desktop\SDN_proj\lab2\image\broad_topo.png" alt="broad_topo" style="zoom:80%;" />

    + Flow Rule

      沿用 part 2 的實作

      + 因為要確保每個 host 知道彼此的  IP 位置所對應的MAC, 所以需要先建立 ARP 傳輸，OUTPUT 設為 ALL 主要是希望可以在任何的 port 都可以接收到怎麼到另一個 host 的位置

        ```json
        {
          "priority": 50001,
          "timeout": 0,
          "isPermanent": true,
          "selector": {
            "criteria": [
              {
                "type": "ETH_TYPE",
                "ethType": "0x806"
              }
            ]
          },
          "treatment": {
            "instructions": [
              {
                "type": "OUTPUT",
                "port": "ALL"
              }
            ]
          }
        }
        ```

      +  s1 和 s3 分別建立，當IPv4_SRC 或 IPv4_DIST 為 10.0.0.0/8 則會 OUTPUT 非 ingress 的 port (也就是會有多個 packet 輸出)

        ```json
        {
          "priority": 50000,
          "timeout": 0,
          "isPermanent": true,
          "selector": {
            "criteria": [
              {
                "type": "ETH_TYPE",
                "ethType": "0x800"
              },
              {
                "type": "IPV4_SRC",
                "ip": "10.0.0.0/8"
              },
              {
                "type": "IPV4_DST",
                "ip": "10.0.0.0/8"
              }
            ]
          },
          "treatment": {
            "instructions": [
              {
                "type": "OUTPUT",
                "port": "ALL"
              }
            ]
          }
        }
        ```

        

      + s2 分別定義 IN_PORT 怎麼轉發

        ```json
        {
          "priority": 50000,
          "timeout": 0,
          "isPermanent": true,
          "selector": {
            "criteria": [
              {
                "type": "IN_PORT",
                "port": "1"
              }
            ]
          },
          "treatment": {
            "instructions": [
              {
                "type": "OUTPUT",
                "port": "2"
              }
            ]
          }
        }
        ```

        

    + 產生 broadcast storm 原因

      當 h1 傳送封包時，因為 OUTPUT 為 ALL 所以會傳輸兩條路徑(分別為紅色和藍色線)，接著當傳輸至 s3 時，因為 s3 的flow rule 也是 OUTPUT ALL 所以當接收到 packet 時會再回頭傳送導致 s1 會再收到相同的 packet 而產生 loop (虛線) 。

      <img src="C:\Users\a3023\Desktop\圖片1.png" alt="圖片1" style="zoom: 80%;" />

    

  + 當遇到 broadcast storm 的情況時觀察的現象

    + link status

      會有多個重複的封包不斷在 topology 中打轉產生 loop，且傳送速率以百萬為每秒計算。

      當 `h1 ping h2` 時，會出現 (DUP!) 的字樣，表示產生速度過快導致

      <img src="C:\Users\a3023\Desktop\SDN_proj\lab2\image\flow-1.png" alt="flow-1" style="zoom:80%;" />

      ![flow-2](C:\Users\a3023\Desktop\SDN_proj\lab2\image\flow-2.png)

      <img src="C:\Users\a3023\Desktop\SDN_proj\lab2\image\dup.png" alt="dup" style="zoom:80%;" />

    + CPUs utilization of VM

      隨著 host 之間傳送 packet 的時間越長， CPU idle state 的比例不斷下降(右圖)

      <img src="C:\Users\a3023\Desktop\SDN_proj\lab2\image\CPU.png" alt="CPU" style="zoom:80%;" />



## Part 4: Trace Reactive Forwarding

+ Activate only “org.onosproject.fwd” and other initially activated APPs. 

+ Use Mininet default topology and let h1 ping h2. 

  ```bash
  sudo mn --controller=remote,127.0.0.1:6653 --switch=ovs,protocols=OpenFlow14
  ```

+ Observe what happens in control and data planes 

  + From the time when h1 pings h2 until h2 receives the first ICMP request 
  + Write down each operation made by control and data planes 
  + Please refer to the ONOS Reactive Forwarding application -- Source Code 

+  Describe what you observed step by step in report

  ![part4-1](C:\Users\a3023\Desktop\SDN_proj\lab2\image\part4-1.png)

  1. h1丟出一個 broadcast ARP Request  (Who has 10.0.0.2? Tell 10.0.0.1) 
  2. 同時 s1 會傳送一個 Open Flow (data 為 ARP request) 去問 c1 (OFPT_PACKET_IN: IN_PORT:1) 
  3. c1 回傳 s1 一個 Open Flow (OFPT_PACKET_OUT: FLOOD)
  4. h2 收到 h1 的 ARP Request 後會回傳 ARP reply  (10.0.0.2 is at 7a:a6:38:13:e7:dd) 
  5. 同時 s1 也會傳送一個 Open Flow (data 為 ARP reply) 去問 c1 (OFPT_PACKET_IN: IN_PORT:2)
  6. c1 回傳 s1  一個 Open Flow (OFPT_PACKET_OUT: OUTPUT:FLOOD)
  7. h1 收到 h2 傳送的 ARP reply，得知 10.0.0.2 在哪個 mac，並傳送 ICMP (Echo ping request) 至 h2
  8. 同時 s1 會傳送一個 Open Flow (data 為 Echo ping request) 去問 c1 (OFPT_PACKET_IN: IN_PORT:1) 
  9. c1 回傳 s1 (OFPT_PACKET_OUT: FLOOD)
  10. h2 收到 **first ICMP request** 後會回傳 ICMP reply
  11. 同時 s1 會傳送一個 Open Flow (data 為 Echo ping reply) 去問 c1 (OFPT_PACKET_IN: IN_PORT:2) 
  12. c1 回傳 s1 (OFPT_PACKET_OUT: FLOOD)



## What you’ve learned or solved

1. app activate Activated org.onosproject.fwd 可以簡寫成 app activate fwd
2. 了解 Openflow 有 Ingress port 和 output port，分別為接收 packet 的 port 和傳送 packet 出去的 port
3. Part 2 其中一項要求是只能有一個 instruction 去傳輸 ARP，不能單純只是使用 IN_PORT 和 OUTPUT 一個特別的 port ，特別去查 SPEC ，找出可使用 `ALL` 來做 broadcast 的操作，有利 Part 3 的呈現
4. Part 2 中的 ipv4_src 和 ipv4_dst 輸入的是一個網域的 ip，非特定某個 host 的 ip，所以在 OUTPUT 時 直接使用 ALL 而非指定 port 可以直接讓兩組 host 可以 ping 的到
5. `curl` RESTFUL GET 時呈現的 json 很醜，所以特別查了一下怎麼顯現 pretty json (json_pp)
6. 第三部分中思考了蠻久的，首先專有名詞 `Broadcast Storm` 沒有很清楚是甚麼，所以上網去了解情境，再到後面的設計沿用了 part 2 的 code 使用 ipv4 來傳輸，因為在設計 json 的時候卡關蠻久的，其中一個發現是若沒有先建立 ARP 傳輸時，IPv4 會無法傳輸，應該是要先知道其他人的 ip 連接在哪裡。
7. 另外，也有觀察到當 `fwd` app activate 且 h1 ping h2 時產生的 flow role 是依據 MAC 的位址去做傳輸的 。

### Observation and Question

在下面中的情境

<img src="C:\Users\a3023\Desktop\SDN_proj\lab2\image\broad_topo.png" alt="broad_topo" style="zoom:80%;" />

在未設計 Rule 時 default link 如下

![see_2](C:\Users\a3023\Desktop\SDN_proj\lab2\image\see_2.png)

若設計的 s2 flow rule 只有單一方向 IN_PORT = 1, OUTPUT = 2 (沒有 IN_PORT = 2, OUTPUT = 1 ) ，則 link 如下 ，會多出一條 

s1:1 -> s3:1 的 flow

<img src="C:\Users\a3023\Desktop\SDN_proj\lab2\image\see_1.png" alt="see_1" style="zoom:80%;" />

若設計的 s2 flow rule 雙方向 ( IN_PORT = 1, OUTPUT = 2 ) & ( IN_PORT = 2, OUTPUT = 1 )

s1:1 <-> s3:1 的 flow 可以互相通，並且 s2 的 link 都只有單一方向

<img src="C:\Users\a3023\Desktop\SDN_proj\lab2\image\see_3.png" alt="see_3" style="zoom:80%;" />

---



#### Tutorial of Curl

+ Create new policy

  ```bash
  curl -u onos:rocks -X POST -H 'Content-Type: application/json' -d @flow1.json 'http://localhost:8181/onos/v1/flows/of:0000000000000001'
  ```

  ```
  curl -I/ -v/ -s/ (Get http reponse)
  ```

  

+ Get added policy

  + All policy

  ```bash
  curl -u onos:rocks -X GET -H 'Accept: application/json' 'http://localhost:8181/onos/v1/flows/of:0000000000000001' | json_pp
  ```

  + Get added specific flow policy

  ```bash
  curl -u onos:rocks -X GET -H 'Accept: application/json' 'http://localhost:8181/onos/v1/flows/of:0000000000000001/49539596291667367' | json_pp
  ```

+ Delete added policy

  ```
  curl -v -u onos:rocks -X DELETE -H 'Accept: application/json' 'http://localhost:8181/onos/v1/flows/of:0000000000000001/49539596291667367'
  ```

  <img src="C:\Users\a3023\Desktop\SDN_proj\lab2\image\in_port.png" alt="in_port" style="zoom:67%;" />

```bash
chmod +x storm.sh
```

