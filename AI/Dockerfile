FROM python:3.11-slim

WORKDIR /app

# Copy project files
COPY requirements.txt .

# Install dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Set environment variables
ENV PYTHONUNBUFFERED=1
ENV OLLAMA_BASE_URL=http://ollama:11434

# Run the AI application
CMD ["celery", "-A", "celery.celery_app", "worker", "--loglevel=info"]
