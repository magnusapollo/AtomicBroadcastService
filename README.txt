1. COMPILING:
	
	Navigate to the src folder.
	Compile using the following command:
		javac edu/utdallas/atomicbroadcastservice/*/*.java edu/utdallas/atomicbroadcastservice/*.java

2. RUNNING:
	Navigate to the src folder.
	Run using the following command:
	java edu.utdallas.atomicbroadcastservice.AtomicBroadcastServiceDemo <node id> <no. of messages sent by each node> <path to config file>
	For ex: java edu.utdallas.atomicbroadcastservice.AtomicBroadcastServiceDemo 0 10 config.txt

NOTE: 
A) Please ensure that the config.txt is present in the src folder. Else create one having the following format:
<Node id><space><hostname><space><port number>
<Node id><space><hostname><space><port number>
<Node id><space><hostname><space><port number>
		:
		:
		.

B) The program should run sequentially in the order mentioned in the config file i.e. The program should be invoked on the machine with node is 0 then on the machine with node id 1 and so on..


3. PROTOCOL: 
	The protocol used for ensuring atomic broadcast and ordered delivery of messages to all nodes is a modificiation of Ricart and Agrawala. For a Node Ni:
		=> On Sending Message: 
			- Broadcast request to all nodes. The request has the sender node id, timestamp and the message to be broadcasted.
		=> On Receiving Message:
				
			- Send reply if:
			a) Have no request unfulfilled of my own.
			b) My request has greater timestamp the request received.
		=> On Receiving all replies:
			- Send deliver message to all nodes.
			- Send deferred replies.
			- Notify self application of delivery.
		=> Terminate if:
			All other nodes are done sending and Ni has delivered all received requests.

4. APPLICATION: The applicaiton used to test the service is a stack(LIFO) implementation with just two methods of insert and delete. 
These methods perform normal stack operations of push and pop.

5. TESTING:
	Each node compares its own delivery log file with the delivery log file of the first node.
	Returns true if contents of the files are same, false otherwise.
