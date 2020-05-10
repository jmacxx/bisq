#!/bin/bash

bitcoin_cli="bitcoin-cli -rpcuser=bisqdao -rpcpassword=bsq -datadir=. -regtest"


function is_connected(){
	sleep 1
	blocks=$($bitcoin_cli getblockcount)
	if [ $? -eq 0 ];then
		echo "yes"
	else
		echo "no"
	fi
}

connected=$(is_connected)
until [ "$connected" == "yes" ];do
	connected=$(is_connected)
done

blocks=$($bitcoin_cli getblockcount)
if [ $? -eq 0 ] && [ $blocks -lt 101 ];then
	echo "$0: found less blocks then 101, generating ..."
	mining_address=$($bitcoin_cli getnewaddress "mining_address")
	$bitcoin_cli generatetoaddress 101 $mining_address
	echo "$0: done, exiting"
	exit 0
fi
echo "$0: nothing to do, exiting"
