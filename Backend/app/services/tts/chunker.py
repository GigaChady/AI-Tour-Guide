import re

def split(text: str) -> list[tuple[int, str]]:
    # Chunking disabled — Edge TTS provides word boundaries natively for the full text,
    # so splitting into sentences is no longer needed for live transcript sync.
    # Re-enable below if parallel synthesis per sentence is needed in the future.
    #
    # parts = re.split(r'[.!?]+', text)
    # result = []
    # chunk_id = 0
    # for part in parts:
    #     chunk = part.strip()
    #     if chunk:
    #         result.append((chunk_id, chunk))
    #         chunk_id += 1
    # return result

    stripped = text.strip()
    return [(0, stripped)] if stripped else []
