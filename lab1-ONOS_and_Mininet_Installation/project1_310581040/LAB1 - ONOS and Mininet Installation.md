# LAB1 - ONOS and Mininet Installation

> 310581040 智能所 簡郁宸

## Part 1 Answer Question

+ Activate ONOS APPs

  1. **When ONOS activates “org.onosproject.openflow,” what are the APPs which it  also activates?** 

     > *   3 org.onosproject.hostprovider         2.7.0    Host Location Provider
     > *   4 org.onosproject.lldpprovider         2.7.0    LLDP Link Provider
     > *   5 org.onosproject.optical-model        2.7.0    Optical Network Model
     > *   6 org.onosproject.openflow-base      2.7.0    OpenFlow Base Provider

     共額外啟動了 `org.onosproject.hostprovider ` 、 `org.onosproject.lldpprovider` 、 `org.onosproject.optical-model  `、 `org.onosproject.openflow-base` 這 4 個 APPs

     <img src="C:\Users\a3023\AppData\Roaming\Typora\typora-user-images\image-20230926164316787.png" alt="image-20230926164316787" style="zoom:80%;" />

  2. **After activating ONOS and running the commands on P.17 and P.20. Will H1 ping H2 successfully?** 

     **Why or why not?**

     > Ref: [Basic ONOS tutorial](https://wiki.onosproject.org/display/ONOS/Basic+ONOS+Tutorial)
     >
     > Well, there are no flows installed on the data-plane, which forward the traffic appropriately. ONOS comes with a simple *Reactive Forwarding* app that installs forwarding flows on demand, but this application is not activated by default.

     **A: No**, 因為在 data-plane 上沒有安裝可以 forward traffic 的 flow。
     
     若要能使的 host 間可以 ping 得到 ，可以激發 ONOS 上的  Reactive Forwarding application (`org.onosproject.fwd`) ，問題便可以解決。 

+ Observe listening port with terminal command “netstat"
  3. **Which TCP port the controller listens for the OpenFlow connection request  from the switch? screenshot** 
  
     **A: 6653** 
  
     從 `devices` 的指令可知 switch 的 port 為 46932 (如下圖)
  
     ![image-20230930013832918](C:\Users\a3023\AppData\Roaming\Typora\typora-user-images\image-20230930013832918.png)
  
     接著讓 controller (c0) ping switch 1(s1)，並在 Wireshark 中觀察出和 s1 port 46932 連接的 port 為 6653 (如下圖)， 6653 便為 controller 的 port
  
     ![image-20230930015020587](C:\Users\a3023\AppData\Roaming\Typora\typora-user-images\image-20230930015020587.png)
  
     
  
  4. **In question 3, which APP enables the controller to listen on the TCP port?**
  
     **A: `org.onosproject.openflow-base`**
  
     最原始的 APP 運作狀態以及 port 使用狀況
  
     <img src="C:\Users\a3023\Desktop\SDN_proj\lab1\image\螢幕擷取畫面 2023-09-30 020153.png" alt="螢幕擷取畫面 2023-09-30 020153" style="zoom: 80%;" />
  
     當 deactivate `org.onosproject.openflow` 時，APP 運作狀態以及 port 使用狀況，可看出 port 6633 和 6653 已經消失
  
     ![螢幕擷取畫面 2023-09-30 020628](C:\Users\a3023\Desktop\SDN_proj\lab1\image\螢幕擷取畫面 2023-09-30 020628.png)
  
     接著啟動一個個被停止運作的 APP ，發現當 `org.onosproject.openflow-base ` activate 且 `org.onosproject.openflow-base` deactivate 時，port 6633 和 6653 開始運作，表示 `org.onosproject.openflow-base` 可以使得 controller 能在 tcp port 上監聽 
  
     ![螢幕擷取畫面 2023-09-30 021333](C:\Users\a3023\Desktop\SDN_proj\lab1\image\螢幕擷取畫面 2023-09-30 021333.png)

## Part 2 Create a custom Topology

+ Write a Python script to build the following topology\

  ```python
  from mininet.topo import Topo
  
  
  class Project1_Topo_310581040( Topo ):
  	def __init__(self):
  		Topo.__init__(self)
  		# Add hosts
  		h1 = self.addHost('h1')
  		h2 = self.addHost('h2')
  		h3 = self.addHost('h3')
  		h4 = self.addHost('h4')
  		h5 = self.addHost('h5')
  
  		# Add switches
  		s1 = self.addSwitch('s1')
  		s2 = self.addSwitch('s2')
  		s3 = self.addSwitch('s3')
  		s4 = self.addSwitch('s4')
  		s5 = self.addSwitch('s5')
  
  		# Add switch/switch
  		self.addLink( s1, s2 )
  		self.addLink( s3, s2 )
  		self.addLink( s4, s2 )
  		self.addLink( s5, s2 )
  
  		# Add host/swtich
  		self.addLink( h1, s1 )
  		self.addLink( h2, s2 )
  		self.addLink( h3, s3 )
  		self.addLink( h4, s4 )
  		self.addLink( h5, s5 )
  
  
  topos = {'topo_part2_310581040': Project1_Topo_310581040 }
  ```
  
  
  
  + Run your Python script and use command “pingall”. 
  
    ```bash
    $ sudo mn --custom=project1_part2_310581040.py \
    --topo=topo_part2_310581040 \
    --controller=remote,ip=127.0.0.1:6653 \
    --switch=ovs,protocols=OpenFlow14
    ```
  
    + 執行過程
  
      <img src="C:\Users\a3023\AppData\Roaming\Typora\typora-user-images\image-20230930013325660.png" alt="image-20230930013325660" style="zoom: 80%;" />
  
  + Then take a **screenshot** of  topology on GUI.
  
    + GUI
  
      <img src="C:\Users\a3023\Desktop\SDN_proj\lab1\image\part2.png" alt="part2" style="zoom:80%;" />



## Part 3 Statically assign Hosts IP Address in Mininet

+ Reuse the topology in part 2

+ Format for manual assignment of host IP address: 

  + ‒ 192.168.0.0/27
  +  ‒ netmask 255.255.255.224

+ Statically assign IP addresses with Python and hand in the Python script you’ve  edited

  ```python
  from mininet.topo import Topo
  
  
  class Project1_Topo_310581040( Topo ):
  	def __init__(self):
  		Topo.__init__(self)
  		# Add hosts
  		h1 = self.addHost('h1', ip = '192.168.0.1/27')
  		h2 = self.addHost('h2', ip = '192.168.0.2/27')
  		h3 = self.addHost('h3', ip = '192.168.0.3/27')
  		h4 = self.addHost('h4', ip = '192.168.0.4/27')
  		h5 = self.addHost('h5', ip = '192.168.0.5/27')
  
  		# Add switches
  		s1 = self.addSwitch('s1')
  		s2 = self.addSwitch('s2')
  		s3 = self.addSwitch('s3')
  		s4 = self.addSwitch('s4')
  		s5 = self.addSwitch('s5')
  
  		# Add switch/switch
  		self.addLink( s1, s2 )
  		self.addLink( s3, s2 )
  		self.addLink( s4, s2 )
  		self.addLink( s5, s2 )
  
  		# Add links
  		self.addLink( h1, s1 )
  		self.addLink( h2, s2 )
  		self.addLink( h3, s3 )
  		self.addLink( h4, s4 )
  		self.addLink( h5, s5 )
  
  
  
  
  topos = {'topo_part3_310581040': Project1_Topo_310581040 }
  ```

   

  + **Screenshots** of manual assignment of host IP address

    <img src="C:\Users\a3023\Desktop\SDN_proj\lab1\image\part3.png" alt="part3" style="zoom:80%;" />

+ Start `mn` with your Python script 

  ```bash
  $ sudo mn --custom=project1_part3_310581040.py \
  --topo=topo_part3_310581040 \
  --controller=remote,ip=127.0.0.1:6653 \
  --switch=ovs,protocols=OpenFlow14
  ```

+ Take screenshots with command  `dump` and  `ifconfig` for all host.

  + `dump`

    <img src="C:\Users\a3023\Desktop\SDN_proj\lab1\image\螢幕擷取畫面 2023-09-30 212247.png" alt="螢幕擷取畫面 2023-09-30 212247" style="zoom: 80%;" />

  + `ifconfig`

    <img src="C:\Users\a3023\Desktop\SDN_proj\lab1\image\螢幕擷取畫面 2023-09-30 214513.png" alt="螢幕擷取畫面 2023-09-30 214513" style="zoom: 80%;" /><img src="C:\Users\a3023\Desktop\SDN_proj\lab1\image\螢幕擷取畫面 2023-09-30 214454.png" alt="螢幕擷取畫面 2023-09-30 214454" style="zoom:80%;" /><img src="C:\Users\a3023\Desktop\SDN_proj\lab1\image\螢幕擷取畫面 2023-09-30 214432.png" alt="螢幕擷取畫面 2023-09-30 214432" style="zoom:80%;" />

## What you’ve learned or solved

+ 首先在最一開始測試時並沒有發現在複製 tutorial 的指令時，將 terminal 換行符號也複製上去，導致後面指令未執行，直接成為輸出導向，讓 ONOS 設置 host 時失敗。

+ 另外，在前面實作 tutorial 時並未發現未啟動`org.onosproject.fwd` 的 APP 時會導致無法使用 pingall，就如第一大題問題所說，在 default 的 data plane 上並沒有連接傳輸的 flow ，故會導致此問題。

+ 在第三部分時 ip 遮罩轉換的部分已經忘得差不多了，特別網路上查了一下喚起了記憶:

  遮罩 255.255.255.224 (11111111.11111111.11111111.11100000) -> $256-224 = 32 = 2^5$  -> 32 - 5 = 27 為 net address
