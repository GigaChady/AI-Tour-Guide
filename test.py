import requests, base64

invoke_url = "https://integrate.api.nvidia.com/v1/chat/completions"
stream = False


headers = {
  "Authorization": "Bearer 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",
  "Accept": "text/event-stream" if stream else "application/json"
}

payload = {
  "model": "deepseek-ai/deepseek-v4-pro",
  "messages": [{"role":"user","content":"What is the diagram of the web service?"}],
  "max_tokens": 512,
  "temperature": 1.00,
  "top_p": 1.00,
  "frequency_penalty": 0.00,
  "presence_penalty": 0.00,
  "stream": stream
}

response = requests.post(invoke_url, headers=headers, json=payload)

if stream:
    for line in response.iter_lines():
        if line:
            print(line.decode("utf-8"))
else:
    print(response.json())