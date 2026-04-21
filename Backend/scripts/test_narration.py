import argparse
import asyncio
import json
import struct

import websockets

'''Wywolujesz
python scripts/test_narration.py --token <token>
python scripts/test_narration.py --token <token> --lat 50.0 --lng 20.0
python scripts/test_narration.py --token <token> --lat 50.0 --lng 20.0 --start-tour
python scripts/test_narration.py --token <token> --session-id <id>  # reconnect to existing tour
'''

WS_URL = "ws://localhost:8000/route/ws"
REDIS_URL = "redis://redis:6379"



async def run(token: str, lat: float = None, lng: float = None) -> None:
    async with websockets.connect(WS_URL) as ws:
        await ws.send(json.dumps({"token": token}))
        msg = json.loads(await ws.recv())
        assert msg["type"] == "ready", f"Expected ready, got: {msg}"
        session_id = msg["session_id"]
        route_id = msg["route_id"]

        r = await aioredis.from_url(REDIS_URL, decode_responses=True)

        if lat is not None and lng is not None:
            await r.xadd("location:events", {
                "session_id": "fd12e30d-0705-40f6-b2da-1a3eb6a0ae56",
                "lat": str(lat),
                "lng": str(lng),
            })
            print(f"Sent lat/lng to Redis: lat={lat}, lng={lng}")

        await r.aclose()

        narration_received = False
        chunks: dict[int, bytes] = {}
        while True:
            try:
                raw = await asyncio.wait_for(ws.recv(), timeout=30)
            except asyncio.TimeoutError:
                print("Timeout waiting for messages")
                break

            if isinstance(raw, bytes):
                chunk_id = struct.unpack(">I", raw[:4])[0]
                print(f"[AUDIO] chunk_id={chunk_id}, bytes={len(raw) - 4}")
                continue

            try:
                msg = json.loads(raw)
            except Exception:
                print(f"Could not decode message: {raw!r}")
                continue

            t = msg.get("type")

            if t == "narration":
                print(f"Received narration: {msg.get('data')}")
                narration_received = True


            elif t in ("error", "detail"):
                print(f"{msg}")
                break

                  


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--token", required=True)
    parser.add_argument("--lat", type=float, default=None, help="Latitude to send to Redis (optional)")
    parser.add_argument("--lng", type=float, default=None, help="Longitude to send to Redis (optional)")
    args = parser.parse_args()
    asyncio.run(run(args.token, args.lat, args.lng))
