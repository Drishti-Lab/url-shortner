# URL Shortener - End-to-End DevOps Pipeline

A Spring Boot URL shortener used to demonstrate a complete DevOps pipeline:
version control, CI, containerization, image registry, and CD to Kubernetes.

## Architecture

```
Developer -> GitHub -> GitHub Actions
                         |-- build + unit tests + JaCoCo   (CI)
                         |-- Docker build -> GHCR + Trivy  (containerization)
                         '-- kubectl apply -> minikube     (CD, self-hosted runner)
                                                 |
                                   Deployment (2 pods) -> StatefulSet (PostgreSQL)
```

## Tech stack
Java 17, Spring Boot 3.3, Spring Data JPA, PostgreSQL, Maven, JUnit 5, Mockito,
JaCoCo, Docker, GitHub Actions, GHCR, Trivy, Kubernetes (minikube).

## API
| Method | Path | Description |
|---|---|---|
| POST | `/api/shorten` | Body `{"url":"https://..."}` -> 201 with `code` and `shortUrl` |
| GET | `/{code}` | 302 redirect to the original URL (counts a click) |
| GET | `/api/stats/{code}` | Click count and metadata |
| GET | `/actuator/health` | Health check |

## Run locally (Docker)
```bash
docker compose up --build
curl -X POST localhost:8080/api/shorten -H 'Content-Type: application/json' \
     -d '{"url":"https://github.com"}'
curl -i localhost:8080/<code>
```

## Run tests
```bash
mvn clean verify          # JaCoCo report: target/site/jacoco/index.html
```
Tests need no database: the service is tested with Mockito, the controller with MockMvc.

## Pipeline (`.github/workflows/ci-cd.yml`)
1. **build-test** - runs on every push and PR: JDK 17, `mvn clean verify`, uploads reports.
2. **docker** - on push to `main`: builds the multi-stage image, pushes to GHCR tagged with
   the commit SHA and `latest`, scans with Trivy.
3. **deploy** - applies `k8s/` manifests to minikube, waits for rollout, runs a smoke test.

### One-time setup for CD
1. Push the repo to GitHub, let the `docker` job run once, then open the package under your
   GitHub profile -> Packages -> `url-shortener` -> Package settings -> set visibility to **Public**
   (so minikube can pull without credentials).
2. Start minikube: `minikube start`.
3. Register a self-hosted runner on the same machine: repo -> Settings -> Actions -> Runners ->
   New self-hosted runner. Make sure `kubectl` points at minikube.
4. Create a repository variable: Settings -> Secrets and variables -> Actions -> Variables ->
   `ENABLE_DEPLOY` = `true`.
5. Push to `main`. Open the app with `minikube service url-shortener --url`.

## Manual Kubernetes deploy
```bash
sed -i 's|ghcr.io/OWNER/url-shortener:latest|ghcr.io/<you>/url-shortener:latest|' k8s/deployment.yaml
kubectl apply -f k8s/
minikube service url-shortener --url
```

## Git workflow
Work on feature branches, open a pull request into `main`; CI must pass before merging.
