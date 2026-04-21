import argparse
import asyncio
import base64
import json

import redis.asyncio as aioredis
import websockets

'''Wywolujesz
python scripts/test_narration.py --token <token> --text "Zamek pochodzi z XIV wieku. Byl siedziba krolow."
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
                "session_id": session_id,
                "lat": str(lat),
                "lng": str(lng),
            })
            print(f"Sent lat/lng to Redis: lat={lat}, lng={lng}")

        await r.aclose()

        print("Waiting for messages from backend (forwarded from Redis)...")
        while True:
            raw = await asyncio.wait_for(ws.recv(), timeout=30)
            print(f"WS RAW: {raw}")
            try:
                msg = json.loads(raw)
            except Exception:
                print("Could not decode message!")
                continue
            t = msg.get("type")
            if t == "narration_transcript":
                print(f"[NARRATION] {msg.get('text')}")
            elif t == "pois":
                print(f"[POIS] {msg.get('data')}")
            elif t in ("error", "detail"):
                print(f"[ERROR/DETAIL] {msg}")
                break
            else:
                print(f"[OTHER] {msg}")

                  


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--token", required=True)
    parser.add_argument("--lat", type=float, default=None, help="Latitude to send to Redis (optional)")
    parser.add_argument("--lng", type=float, default=None, help="Longitude to send to Redis (optional)")
    args = parser.parse_args()
    asyncio.run(run(args.token, args.lat, args.lng))
