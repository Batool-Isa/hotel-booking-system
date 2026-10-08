import asyncio, json, sys, urllib.request
import websockets

BASE = "localhost:8000"

def login(email, password):
    req = urllib.request.Request(
        f"http://{BASE}/auth/users/login",
        data=json.dumps({"email": email, "password": password}).encode(),
        headers={"Content-Type": "application/json"},
    )
    return json.load(urllib.request.urlopen(req))["message"]

def frame(command, headers=None, body=""):
    lines = [command] + [f"{k}:{v}" for k, v in (headers or {}).items()]
    return "\n".join(lines) + "\n\n" + body + "\x00"

async def main(email, password, destinations):
    token = login(email, password)
    async with websockets.connect(f"ws://{BASE}/ws") as ws:
        await ws.send(frame("CONNECT", {"accept-version": "1.2", "heart-beat": "0,0",
                                        "Authorization": f"Bearer {token}"}))
        reply = await ws.recv()
        print(reply.split("\n")[0])
        if not reply.startswith("CONNECTED"):
            print(reply)
            return
        for i, dest in enumerate(destinations):
            await ws.send(frame("SUBSCRIBE", {"id": f"sub-{i}", "destination": dest}))
            print("subscribed:", dest)
        print("listening... (Ctrl+C to stop)")
        async for message in ws:
            print(message.replace("\x00", ""))
            print("-" * 40)

if __name__ == "__main__":
    if len(sys.argv) < 4:
        print("usage: py ws_listen.py EMAIL PASSWORD DESTINATION [DESTINATION ...]")
        sys.exit(1)
    asyncio.run(main(sys.argv[1], sys.argv[2], sys.argv[3:]))
