import re

def split(text: str) -> list[tuple[int, str]]: 
    '''We want to chunk the text for perpose of implementation of live transcription. 
    Id is needed so we can keep track of the order of the chunks.'''
    parts = re.split(r'[.!?]+', text)
    result = []
    chunk_id = 0
    for part in parts:
        chunk = part.strip()
        if chunk:
            result.append((chunk_id, chunk))
            chunk_id += 1
    return result
