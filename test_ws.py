from websocket import create_connection

ws = create_connection("ws://localhost:8090/ws")

while True: 
    result = ws.recv()
    print(result)
