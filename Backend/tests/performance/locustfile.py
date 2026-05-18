from locust import HttpUser, between, task


class BackendSmokeUser(HttpUser):
    wait_time = between(0.2, 1.0)

    @task(3)
    def health(self):
        self.client.get("/health", name="GET /health")

    @task(2)
    def readiness(self):
        self.client.get("/ready", name="GET /ready")

    @task(1)
    def version(self):
        self.client.get("/version", name="GET /version")
