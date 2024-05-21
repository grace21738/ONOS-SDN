all:
	docker compose up -d
	sudo ovs-vsctl add-br ovs-br0
	#sudo ifconfig ovs-br0 172.20.0.1 netmask 255.255.255.0 up
	sudo ovs-docker add-port ovs-br0 eth2 R1 --ipaddress=172.20.0.2/16
	sudo ovs-docker add-port ovs-br0 eth2 R2 --ipaddress=172.20.0.3/16
	sudo ovs-docker add-port ovs-br0 eth2 R3 --ipaddress=172.20.0.4/16
	sudo ovs-vsctl set bridge ovs-br0 protocols=OpenFlow13
	sudo ovs-vsctl set-controller ovs-br0 tcp:127.0.0.1:6653
	
clean:
	docker compose down
	sudo ovs-vsctl del-br ovs-br0
