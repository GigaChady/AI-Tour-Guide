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
REDIS_URL = "redis://localhost:6379"


async def run(token: str, text: str) -> None:
    async with websockets.connect(WS_URL) as ws:

        await ws.send(json.dumps({"token": token}))
        msg = json.loads(await ws.recv())
        assert msg["type"] == "ready", f"Expected ready, got: {msg}"
        session_id = msg["session_id"]
        route_id = msg["route_id"]

        r = await aioredis.from_url(REDIS_URL, decode_responses=True)
        payload = json.dumps({"type": "narration", "text": text})
        subscribers = await r.publish(f"tour:{session_id}", payload)
        await r.aclose()

        chunks: dict[int, bytes] = {}
        while True:
            raw = await asyncio.wait_for(ws.recv(), timeout=30)
            msg = json.loads(raw)
            t = msg.get("type")

            if t == "narration_transcript":
                for entry in msg["transcript"]:
                    print(f"Chunk {entry['chunk_id']}: {entry['text']}")

            elif t == "narration_chunk":
                cid = msg["chunk_id"]
                audio = base64.b64decode(msg["audio"])
                words = msg.get("words", [])
                chunks[cid] = audio
                print(f"\n {len(audio)} bytes | {len(words)} words")
                for w in words:
                    print(f"  {w['offset_ms']:7.1f}ms  +{w['duration_ms']:.1f}ms  \"{w['text']}\"")

            elif t == "narration_done":
                print(f"\n{len(chunks)} chunks received")
                break

            elif t in ("error", "detail"):
                print(f"{msg}")
                break

        if chunks:
            merged = b"".join(chunks[i] for i in sorted(chunks))
            out = "test_narration_output.mp3"
            with open(out, "wb") as f:
                f.write(merged)
            print(f"{out} ({len(merged)} bytes)")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--token", required=True)
    parser.add_argument(
        "--text",
        default="Zamek pochodzi z XIV wieku. Byl siedziba krolow polskich. Zwiedzanie trwa okolo godziny.",
    )
    args = parser.parse_args()
    asyncio.run(run(args.token, args.text))
