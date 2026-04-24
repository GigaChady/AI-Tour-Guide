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


async def run(token: str, lat: float = None, lng: float = None, session_id: str = None, start_tour: bool = False) -> None:
    connect_msg = {"token": token}
    if session_id:
        connect_msg["session_id"] = session_id

    async with websockets.connect(WS_URL) as ws:
        await ws.send(json.dumps(connect_msg))
        raw = json.loads(await ws.recv())

        if "session_id" not in raw:
            print(f"Unexpected initial message: {raw}")
            return

        session_id = raw["session_id"]
        route_id = raw.get("route_id")
        mode = "tour" if route_id else "preview"
        print(f"Connected: mode={mode}, session_id={session_id}, route_id={route_id}")

        if start_tour and mode == "preview":
            await ws.send(json.dumps({"type": "start_tour"}))
            tour_msg = json.loads(await ws.recv())
            route_id = tour_msg.get("route_id")
            mode = "tour"
            print(f"Tour started: route_id={route_id}")

        if lat is not None and lng is not None:
            await ws.send(json.dumps({"lat": lat, "lng": lng}))
            print(f"Sent lat/lng via WS: lat={lat}, lng={lng}")

        print("Waiting for messages from backend...")
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
            if t == "pois":
                print(f"[POIS] {msg.get('data')}")
            elif t == "narration_transcript":
                print(f"[NARRATION_TRANSCRIPT] {msg.get('transcript')}")
            elif t == "narration_words":
                print(f"[NARRATION_WORDS] chunk_id={msg.get('chunk_id')}, words={msg.get('words')}")
            elif t == "narration_done":
                print("[NARRATION_DONE]")
            elif t == "session_ended":
                print(f"[SESSION_ENDED] reason={msg.get('reason')}")
                break
            elif "detail" in msg:
                print(f"[ERROR] {msg}")
                break
            else:
                print(f"[OTHER] {msg}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--token", required=True)
    parser.add_argument("--lat", type=float, default=None)
    parser.add_argument("--lng", type=float, default=None)
    parser.add_argument("--session-id", default=None, help="Reconnect to an existing tour session")
    parser.add_argument("--start-tour", action="store_true", help="Send start_tour after connecting in preview mode")
    args = parser.parse_args()
    asyncio.run(run(args.token, args.lat, args.lng, args.session_id, args.start_tour))
