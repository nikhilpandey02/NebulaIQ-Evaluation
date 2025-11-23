# Distributed Tracing Fundamentals (Simple & Detailed)

## 1. Why Distributed Tracing?

### Monolith
In a monolithic application, everything runs inside one big program.  
If a request is slow, you can see the whole journey in one stack trace:

```
Controller → Service → Repository → Database
```

Debugging is simple because everything happens inside a single process.

---

### Microservices
In microservices, a single request travels across many services:

```
Client → API Gateway → Service A → Service B → Service C → Database
```

Each service has:
- its own logs
- its own metrics
- its own errors

But nothing shows the **full journey** of a request.

This makes debugging slow requests and errors much harder.

---

### Why we need Distributed Tracing
Distributed tracing gives you:

- Complete journey of a request  
- Where time is spent  
- Which service is slow  
- Where an error happened  
- How services call each other  
- End-to-end visibility  

Distributed tracing = **Google Maps for your microservices**.

---

## 2. Core Concepts

### 1) Trace
A **trace** is the full journey of one request across the system.

Example:
```
GET /order/123
```

If this request passes through 3 services, all those steps combined form **one trace**.

---

### 2) Span
A **span** is one specific step inside the trace.

Examples:
- Service A receives `/order/123`
- Service A makes an HTTP call to Service B
- Service B calls Service C
- Service C runs a DB query

A trace = multiple spans linked together.

---

### 3) Trace ID
A unique ID shared by **all spans** in a trace.

Example:
```
traceId = 4bf92f3577b34da6a3ce929d0e0e4736
```

If multiple spans share this traceId → they belong to the same request.

---

### 4) Span ID
A unique ID for each single span.

Example:
```
spanId = 00f067aa0ba902b7
```

---

### 5) Parent → Child Relationship
This shows the structure of the trace.

Example:
```
Span A (Service A)
   └── Span B (Service B)
          └── Span C (Service C)
```

This forms a **tree of spans**.

---

### 6) Tags / Attributes
Metadata stored inside a span as key-value pairs.

Examples:
```
http.method = GET
http.status_code = 200
db.statement = "SELECT * FROM orders"
```

They help explain what happened in that span.

---

### 7) Events (Logs inside a Span)
These are small, time-stamped messages attached to a span.

Examples:
```
cache.miss
db.query.started
exception
```

They provide important moments inside a span, not full logs.

---

## 3. Trace Context Propagation

Propagation means:

**How does trace-id and span-id travel between services?**

When Service A calls Service B, it must send tracing headers.

Three major standards:

---

### A) W3C Trace Context (Most Common)

Header:
```
traceparent
```

Example:
```
traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01
```

Parts:
- `00` → version  
- `traceId` → same for whole trace  
- `spanId` → id of caller span  
- `01` → sampled flag  

Optional:
```
tracestate: vendor-specific data
```

---

### B) B3 Propagation (Zipkin)
Older system, still used.

Headers:
```
X-B3-TraceId
X-B3-SpanId
X-B3-ParentSpanId
X-B3-Sampled
```

---

### C) gRPC / Kafka / RabbitMQ

#### gRPC
Trace context goes inside **metadata**.

#### Kafka / RabbitMQ
Trace context goes inside **message headers**.

Example:
```
traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-efabcd1293bc0987-01
```

---

## 4. Baggage

Baggage = tiny key/value pairs that travel with the trace.

Example:
```
baggage: region=IN, experiment=green
```

Used for:
- tenant id
- routing info
- experiment ID

Should be:
- small  
- non-sensitive  
- low-cardinality  

---

## 5. Span Structure

A span normally contains:

- traceId  
- spanId  
- parentSpanId  
- name  
- kind (server, client, internal)  
- start time  
- end time  
- duration  
- status (OK or ERROR)  
- attributes  
- events  

---

### Example Span JSON

```json
{
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "spanId": "00f067aa0ba902b7",
  "parentSpanId": "a3ce929d0e0e4736",
  "name": "GET /order/123",
  "kind": "SERVER",
  "startTime": 1690000000000,
  "endTime": 1690000000230,
  "attributes": {
    "http.method": "GET",
    "http.status_code": 200
  },
  "events": [
    { "name": "cache.miss" },
    { "name": "db.query", "attributes": { "sql": "SELECT ..." } }
  ]
}
```

---

## 6. How Context Flows Between Services

Diagram:

```
Client
   |
   v
Service A (creates traceId = T1)
   |
   |-- sends header: traceparent: 00-T1-S1-01
   |
   v
Service B (continues same trace)
   |
   |-- sends header: traceparent: 00-T1-S2-01
   |
   v
Service C (child of Service B span)
```

All services share the same **traceId**.

---

## 7. Summary

- Trace = full journey  
- Span = one step  
- Trace ID = same for all spans  
- Span ID = unique per step  
- Parent → child = links spans  
- Attributes = metadata  
- Events = important moments  
- Propagation = passing traceparent  
- Baggage = small values passed across services  

---

# OpenTelemetry Deep Dive (Simple & Detailed)

## 1. What is OpenTelemetry (OTel)?

OpenTelemetry is a single, open-source system for collecting **traces, metrics, and logs** from your application.

- It is **vendor-neutral** → works with Jaeger, Tempo, DataDog, New Relic, anything.
- It replaced **OpenTracing** and **OpenCensus**.
- Its goal: **instrument once → send data anywhere**.

---

## 2. OTel Architecture

OTel has 3 main components:

### A) OTel API  
How you write spans/metrics/logs in your code.

### B) OTel SDK  
The actual implementation that:
- creates spans  
- stores them temporarily  
- batches them  
- exports them to a collector  

### C) OTel Collector  
A standalone service that:
- receives telemetry  
- processes it  
- exports it to backends  

```
[App → OTel SDK] → OTLP → [OTel Collector] → Jaeger / Tempo / DataDog
```

---

## 3. OTLP (OpenTelemetry Protocol)

OTLP is how apps send telemetry to the collector.

- Uses **protobuf**  
- Sent over **gRPC** or **HTTP**  

Trace data is structured as:
- `ResourceSpans`
  - `ScopeSpans`
    - `Span`

---

## 4. Instrumentation

There are two ways to instrument your app:

---

### A) Auto-Instrumentation

- Works with Java, Python, Node, Go, etc.
- No code changes
- Uses:
  - Java → bytecode manipulation  
  - Python/Node → monkey patching  

Pros:
- Fast and easy  
- Captures HTTP, DB, frameworks automatically  

Cons:
- Less control  
- May miss custom logic  

---

### B) Manual Instrumentation

You create spans in your code:

```java
Span span = tracer.spanBuilder("processOrder").startSpan();
try {
    span.setAttribute("order.id", 123);
} finally {
    span.end();
}
```

Pros:
- Full control  
Cons:
- More code  

---

## 5. Sampling Strategies

Sampling reduces data volume & cost.

---

### A) Head-Based Sampling  
Decision is made at start of trace.

Types:
- Probabilistic (e.g., 1% sample)
- Rate limit (keep only X traces/sec)

Pros: cheap  
Cons: may miss errors  

---

### B) Tail-Based Sampling  
Decision is made after the trace ends.

Examples:
- keep all errors  
- keep slow traces  
- drop normal traces  

Pros: best debugging data  
Cons: needs buffering  

---

## 6. OTel Collector Pipeline

```
[Receiver] → [Processors] → [Exporters]
```

**Receivers**: OTLP, Jaeger, Zipkin  
**Processors**: batch, sampling, filtering  
**Exporters**: Jaeger, Tempo, Prometheus, S3  

---

## 7. Practical Steps (How to Instrument an App)

1. Add OTel SDK + agent  
2. Configure exporter (OTLP → Collector)  
3. Start Collector  
4. Run app  
5. Open Jaeger/Tempo and view traces  

---

## 8. Example Collector Pipeline

```yaml
receivers:
  otlp:
    protocols:
      grpc:
      http:

processors:
  batch:
  tail_sampling:
    policies:
      - type: error
      - type: latency
        threshold_ms: 1000

exporters:
  jaeger:
    endpoint: http://localhost:14268/api/traces

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch, tail_sampling]
      exporters: [jaeger]
```

---

## 9. Diagrams

### Simple Architecture

```
 Application
    ↓
 OTel SDK
    ↓ OTLP
 OTel Collector
    ↓
Jaeger / Tempo / Datadog
```

---

## 10. Summary

- OTel is the standard way to collect traces/metrics/logs  
- Auto-instrumentation = quick and easy  
- Manual instrumentation = powerful  
- Sampling helps reduce cost  
- Collector centralizes all processing  

---


# Section 3: Trace Storage & Query  
### Easy English | Simple Diagrams | Beginner Friendly

---

# ⭐ 1. Introduction

This part explains **how tracing systems (Jaeger / Tempo) store traces**,  
**how we find traces later**, and **how service maps are built**.

I will explain everything in **simple language**, like you speak.

---

# ⭐ 2. Trace Storage Overview

A trace = full journey of a request.  
A trace has many spans.  
Storage must save spans in a way that we can search them later.

Tracing systems store spans **by:**
- trace ID  
- service name  
- tags (like http.status_code)  

There are two major systems:

- **Jaeger** → good search, higher cost  
- **Tempo** → cheap storage, huge scale  

---

# ⭐ 3. Jaeger Storage (Easy Explanation)

Jaeger can use these databases:

### ✅ Cassandra  
- Saves spans as rows  
- Good for high write traffic  
- Scales horizontally  
- Indexes by **trace ID**, **service name**, **operation**

### ✅ Elasticsearch  
- Saves spans as documents  
- Very strong search  
- Supports tag search  
- Good for debugging slow/error traces  

### ✅ Kafka (only for ingestion)  
- Not long-term storage  
- Used as buffer before saving to ES/Cassandra  

---

## ✔ How Jaeger Stores Data (Simple)

```
traceId → points to all spans in that trace
service name → list of spans from that service
tags → searchable (ex: error=true)
```

### Diagram

```
Your App → OTel → Jaeger Collector
           ↓
      Storage (ES / Cassandra)
           ↓
       Jaeger UI
```

---

# ⭐ 4. Tempo Storage (Easy Explanation)

Tempo uses **object storage**, not heavy databases.

Examples:
- Amazon S3  
- Google Cloud Storage  
- Azure Blob  

### Why?
Because object storage is **very cheap** and **infinite scale**.

### How it stores data:
- Spans are batched into **blocks**
- Blocks are compressed
- Uploaded to S3/GCS
- Small index created for trace IDs

### Diagram

```
App → OTel → Tempo Distributor
               ↓
           Tempo Ingester
               ↓
      Object Storage (S3)
               ↓
           Tempo Query
               ↓
            Grafana
```

### Good things:
✔ Very cheap  
✔ Handles huge traffic  
✔ Simple to maintain  

### Limitations:
✖ Tag searching is limited  
✖ Queries can be slower  

---

# ⭐ 5. Indexing (How Searches Work)

Indexes help us find traces faster.

### Types of indexes:

### 1️⃣ Trace ID Index  
- Fastest  
- If you know trace ID, lookup is instant  

### 2️⃣ Service Name Index  
Find all traces from a service:

```
service.name = "service-a"
```

### 3️⃣ Tag Index  
Useful for debugging:

```
http.status_code = 500
error = true
db.system = mysql
```

### Trade‑Off  
- More indexes → fast search → more storage cost  
- Less indexes → cheap → slower search  

---

# ⭐ 6. Finding Traces (Query Capabilities)

### ✔ A) Find by Trace ID

```
traceId = abc123
```

Fastest lookup.

---

### ✔ B) Find by Service

```
service.name = "payment-service"
```

---

### ✔ C) Find by Tag

```
http.status_code = 500
```

Used for:
- finding errors  
- filtering slow paths  

---

### ✔ D) Find by Latency

```
span.duration > 1000ms
```

Shows slow traces.

---

### ✔ E) Full-text Tag Search (Jaeger)

If span has text attributes:

```
message = "failed payment due to timeout"
```

Jaeger (Elasticsearch) can search full text.

Tempo cannot do this.

---

# ⭐ 7. Trace Aggregation

Aggregation means **combining many traces** to understand patterns.

We can compute:
- request count  
- error rate  
- latency percentiles (P95, P99)  

This is how tracing → metrics conversion happens.

### Example:
If service-a has 100 traces with:
- 10 errors  
- 90 success  

Error rate = 10%

---

# ⭐ 8. How Service Maps Are Built

A service map shows how services call each other:

```
service-a → service-b → service-c
```

### How system builds it:

1. Read spans  
2. Look at attributes:
   - peer.service  
   - http.url  
   - rpc.system  
3. Connect parent → child  
4. Remove duplicates  
5. Draw graph  

---

# ⭐ Simple Flowchart (Service Map Generation)

```
[Start]  
   ↓  
[Read All Spans]  
   ↓  
[Check span attributes: peer.service, rpc.service]  
   ↓  
[Create edge: parent → child]  
   ↓  
[Merge duplicate edges]  
   ↓  
[Build graph]  
   ↓  
[Show service map]
```

---

# ⭐ 9. Summary (Easy English)

- Jaeger uses Cassandra/Elasticsearch → better search  
- Tempo uses object storage → cheaper, huge scale  
- Indexes help find spans quickly  
- You can search by trace ID, service, tags, latency  
- Service maps come from span relationships  
- Trade-off = cost vs search power

---


# Section 4: Cross-Signal Correlation  
### Easy English | Simple Diagrams | Beginner Friendly

---

# ⭐ 1. Introduction

Cross-signal correlation means **connecting metrics, logs, and traces** so you can find the root cause faster.

Example problems:
- Metric says **high latency** → which trace was slow?  
- Log says **error** → which trace created this log?  
- Trace says **DB slow** → what were CPU metrics at that time?

This section teaches:
- How logs ↔ traces connect  
- How metrics ↔ traces connect  
- How tools (Grafana, DataDog) unify signals  

---

# ⭐ 2. The Correlation Problem (Easy Explanation)

### Problem 1: High latency metric  
User sees:

```
p99 latency = 2 seconds
```

They want to know:

👉 **Which exact trace was slow?**

---

### Problem 2: Error logs  
A log shows:

```
Payment failed for user 55
```

User wants:

👉 **Which trace did this log belong to?**

---

### Problem 3: Slow trace  
A trace shows:

```
DB query took 600ms
```

User wants:

👉 **What were CPU/memory metrics at that moment?**

---

Cross-signal correlation solves all of these.

---

# ⭐ 3. Linking Traces ↔ Logs

Logs must include:

- traceId  
- spanId  

### Example structured log:

```json
{
  "level": "error",
  "msg": "Payment failed",
  "traceId": "abc123",
  "spanId": "def456"
}
```

### How it works:

✔ App gets a trace context  
✔ Logging framework injects traceId & spanId into logs  
✔ Logs stored in Elasticsearch / Loki / DataDog  
✔ User searches “traceId=abc123” → sees related logs  

---

## 🔹 From trace → logs

User opens a trace in Jaeger:

```
traceId = abc123
```

UI shows button:

👉 **Show related logs**

Logs are fetched using:
```
traceId=abc123
```

---

## 🔹 From logs → trace

A log contains traceId.

User clicks “View Trace” → opens Jaeger with:

```
http://localhost:16686/trace/abc123
```

---

# ⭐ 4. Linking Traces ↔ Metrics (Exemplars)

Prometheus supports **exemplars**.

An exemplar attaches a **trace ID** to a **metric data point**.

Example:

Metric:  
```
http_request_duration_seconds_bucket
```

Exemplar:
```
value: 1.2 seconds
traceId: abc123
```

### Meaning:
"This slow request (1.2s) is represented by trace abc123."

Grafana shows a **dot** on the graph → click → open trace.

---

## 🔹 Why not every metric has a trace?

Because:
- too much data  
- too expensive  
- sampling is needed  

So:
✔ Only slow / important metrics have trace ID  
✔ Called **exemplars**  

---

# ⭐ 5. Linking Metrics ↔ Traces (Manual Query)

If NO exemplars, dashboard → find traces manually:

Steps:

1. User sees metric spike at **10:15 - 10:17**  
2. Open Jaeger  
3. Filter by:
   - service name  
   - time range  
   - errors  
4. Inspect slow traces  

---

# ⭐ 6. Unified View (Grafana, DataDog)

Modern tools unify signals automatically.

### ⭐ Grafana

Grafana can connect:

- Prometheus (metrics)  
- Tempo/Jaeger (traces)  
- Loki (logs)  

Using the same **traceId**.

### ⭐ DataDog

DataDog automatically injects:

- traceId into logs  
- trace exemplars into metrics  
- unique IDs across systems  

So user can move:

metrics → logs → traces → logs → services

---

# ⭐ 7. Implementation Guide (Step-by-Step)

### ✔ Step 1: Enable trace context propagation  
OTel agent automatically provides traceId + spanId.

### ✔ Step 2: Configure logging format  
Include:

```
traceId = Span.current().getSpanContext().getTraceId()
spanId = Span.current().getSpanContext().getSpanId()
```

### ✔ Step 3: Push logs to a backend  
- Elastic  
- Loki  
- DataDog Logs  

### ✔ Step 4: Add exemplars (optional)  
In your code:

```java
Histogram metrics = ...
metrics.observeWithExemplar(latency, traceId);
```

### ✔ Step 5: Configure UI linking  
Grafana “Explore” mode supports linking logs ↔ traces ↔ metrics.

---

# ⭐ 8. Benefits & Limitations

### ⭐ Benefits
- Faster debugging  
- Can jump between signals easily  
- Helpful for SRE & backend teams  
- Reduce MTTR (mean time to repair)  

### ⭐ Limitations
- Requires consistent IDs everywhere  
- Some systems (Tempo) have limited tag search  
- Logs without trace IDs cannot be correlated  
- Cross-tool integration can be tricky  

---

# ⭐ 9. Summary (Easy English)

- Always log traceId + spanId  
- Use exemplars for linking metrics → traces  
- Use structured logs (JSON)  
- Jaeger/Tempo + Prometheus + Loki + Grafana = full correlation  
- Goal: One click → jump between logs, metrics, traces  

---


# Section 5: Root Cause Analysis Using Traces  
### Easy English | Simple Diagrams | Beginner-Friendly

---

# ⭐ 1. Introduction

Root Cause Analysis (RCA) using distributed traces means:
- Finding WHICH part of the system caused slow latency,
- WHICH service failed,
- WHERE errors started,
- WHICH dependency is the bottleneck.

Traces give a full request journey → so they are perfect for debugging.

This section explains:
- How to find bottleneck spans
- How to do critical-path analysis
- How to follow error propagation
- How to use service maps for RCA
- Step-by-step RCA workflow

All in simple English.

---

# ⭐ 2. Finding Bottleneck Spans

A trace contains multiple spans:

Example:

```
service-a 150ms
 └── service-b 800ms (slow)
       └── service-c 20ms
```

### Bottleneck Span = span with biggest duration  
In this example → **service-b** is slow.

### How to detect:
- Look at each span's duration
- Sort by duration
- Largest one = bottleneck

Tools like Jaeger highlight the slowest span.

---

# ⭐ 3. Critical Path Analysis

Requests can run **in parallel**.

Example:

```
service-a
 ├── call to service-b (500ms)
 └── call to service-c (200ms)
```

Even though total spans = 700ms, request latency = 500ms.

### Critical Path = the path with longest duration  
= the path that defines total latency.

In this example:  
**service-a → service-b**

### Why critical path matters:
- Optimizing other spans gives NO improvement.
- Only critical path matters for user latency.

---

# ⭐ 4. Error Propagation

Errors flow from child → parent.

Example:

```
service-c (error: DB timeout)
      ↓
service-b (error because child failed)
      ↓
service-a (returns 500 to user)
```

### How to analyze:
1. Look for spans with error tag  
2. Check where the FIRST error occurred  
3. Trace upward to see who caused it  
4. That is your true root cause  

---

# ⭐ 5. Dependency Analysis

Traces tell you which services depend on which.

Example:

```
service-a → service-b → service-c → MySQL
```

### RCA questions:
- Did MySQL slow down?
- Did service-b have a retry loop?
- Did service-c do heavy queries?

You investigate based on dependencies.

Service maps help visualize this.

---

# ⭐ 6. Step-by-Step RCA Workflow

### ⭐ Scenario  
Alert:  
```
API /order latency spiked to 2 seconds
```

### Step 1 — Open recent slow traces  
Filter:
- service = order-service  
- duration > 1 second  

### Step 2 — Look at whole trace timeline  
Find which child span is slow.

### Step 3 — Identify bottleneck span  
Example:
```
payment-service → 1.5 seconds
```

### Step 4 — Check attributes  
Look at tags:
- db.statement
- http.status_code
- retry_count
- cache_miss = true

### Step 5 — Check logs (traceId present)  
From this span, open logs:

Example log:
```
Payment timeout: upstream bank API down
```

### Step 6 — Correlate with metrics  
Check Prometheus:
- payment-service latency
- payment-service errors
- DB CPU usage

### Step 7 — Validate root cause  
If metrics + logs + trace all show:
```
bank API latency = 1500ms
```

Then root cause = external API slowdown.

---

# ⭐ 7. Simple RCA Diagram

```
[Alert: High Latency]
        ↓
  [Look at Trace]
        ↓
 [Find Slow Span]
        ↓
[Check Span Attributes]
        ↓
   [Check Logs]
        ↓
 [Check Metrics]
        ↓
[Identify Real Root Cause]
```

---

# ⭐ 8. Real Examples

### Example 1 — Database Slow  
Trace shows DB query = 800ms.  
Logs show "slow query".  
Metrics show DB CPU high.

Root cause → DB.

---

### Example 2 — Cache Miss  
Trace tag:
```
cache.hit = false
```
Cache slow → hits DB → high latency.

Root cause → cache miss.

---

### Example 3 — Downstream API failure  
Trace shows:
```
payment-service → timeout
```

Logs show error.  
Metrics show payment latency spike.

Root cause → external dependency.

---

# ⭐ 9. Benefits of Trace-Based RCA

- Much faster than logs-only debugging  
- Shows full request timeline  
- Shows exact component that failed  
- Shows order of dependency calls  
- Helps fix performance bottlenecks  

---

# ⭐ 10. Limitations

- Requires good instrumentation  
- Missing spans can break analysis  
- Needs good trace sampling  
- Harder if trace IDs not in logs  
- Tempo has limited tag search  

---

# Distributed Tracing Deep Dive 

# 1. Introduction

Modern microservices break a single request into multiple network hops — API → Service → DB → External API → Cache.  
Traditional logs are *not enough* to understand the end-to-end flow.

**Distributed tracing** lets us follow a single request across multiple services, using:

- Trace IDs  
- Span IDs  
- Context propagation  
- Timelines  
- Tags & logs  

This project demonstrates a full working system with 3 microservices:

- **Service A** — Entry point (/order/{id})  
- **Service B** — Payment service  
- **Service C** — Database or external lookup  

All three are centrally traced using **OpenTelemetry Java Agent** and visualized using **Jaeger**.

---

# 2. Why Distributed Tracing

Without distributed tracing:
- You cannot tell which microservice is slow  
- Logs are isolated  
- Hard to debug cascading failures  
- Hard to identify root cause  

With distributed tracing:
- **Full request flow visualization**  
- **Latency breakdown** across services  
- **Which span took longest**  
- **Dependencies between services**  
- **RCA (root-cause-analysis) becomes easy**  

---

# 3. Architecture Used in This Demo

Client → Service A → Service B → Service C → DB

Service A receives `/order/{id}`  
Service B receives `/payment/{id}`  
Service C receives `/db/{id}`  

### Diagram

+----------+ +-----------+ +-----------+ +-------+
| Client | ---> | Service A | ---> | Service B | ---> | C |
+----------+ +-----------+ +-----------+ +-------+


**Every hop carries the trace context** using W3C Traceparent headers.

---

# 4. OpenTelemetry Architecture (Deep Dive)

OpenTelemetry has 4 major components:

## ✅ 1. **Instrumentation (SDK or Auto-Instrumentation)**  
In our case:
opentelemetry-javaagent.jar

This automatically instruments:
- HTTP clients  
- Spring Boot  
- Web requests  
- RestTemplate / WebClient  

## ✅ 2. **Context Propagation**  
Each service extracts and injects trace headers:
traceparent: 00-<trace-id>-<span-id>-01

This ensures all spans belong to the same trace.

## ✅ 3. **Exporters**  
We export via OTLP (OpenTelemetry Protocol):
http://localhost:4318

## ✅ 4. **Backend (Jaeger)**  
Jaeger stores:
- Traces  
- Spans  
- Tags  
- Events  

And visualizes the traces.

---

# 5. How Traces Flow

1. Client sends request to Service A  
2. OpenTelemetry creates root span  
3. Service A calls Service B → new child span  
4. Service B calls Service C → new nested span  
5. Service C queries database  
6. Spans exported to OTel endpoint (4318)  
7. Jaeger renders the trace  

---

# 6. Trace Storage, Querying & Analysis

Jaeger stores spans in memory (for demo).

### Key Jaeger features used:
- Search traces by service  
- Filter by duration  
- Detailed span timeline  
- Service dependency graph  
- Deep dependency graph  
- Span tags and logs  

<img width="1920" height="1080" alt="Screenshot (245)" src="https://github.com/user-attachments/assets/25a96780-9b34-4310-a766-876746246399" />


---

# 7. Cross-Signal Correlation

Cross-signal correlation improves observability by linking:

### ✔️ Logs ↔ Traces  
Add traceId + spanId in logs:

INFO [traceId=abc123 spanId=def456] Calling service B

Then you can:
- Search logs using trace ID  
- Jump from logs → trace  
- Jump from trace → logs  

### ✔️ Metrics ↔ Traces  
Prometheus exemplars (optional):

- High latency metric → find associated trace  
- Helps identify "why is my p95 high?"

### ✔️ Traces ↔ Metrics ↔ Logs  
This triad enables powerful debugging.

---

# 8. Multi-Service Demo  
## All services built using **Spring Boot 3 + Java 21**

### ### Service A  
Endpoint:  
GET /order/{id}

Calls Service B:
http://localhost:8082/payment/{id}

### Service B  
Endpoint:
GET /payment/{id}

Calls Service C:
http://localhost:8083/db/{id}

### Service C  
Endpoint:
GET /db/{id}

Introduces artificial delay using:
```java
Thread.sleep(1200);
java -javaagent:C:\Users\nikhi\otel\opentelemetry-javaagent.jar ^
     -Dotel.exporter.otlp.endpoint=http://localhost:4318 ^
     -Dotel.service.name=service-a ^
     -jar target/serviceA-0.0.1-SNAPSHOT.jar

<img width="1920" height="1080" alt="Screenshot (246)" src="https://github.com/user-attachments/assets/b06c1b42-d1e3-4700-b00f-80987c8992e8" />
<img width="1920" height="1080" alt="Screenshot (248)" src="https://github.com/user-attachments/assets/8e2ebc8c-8d7c-447c-803a-4e7943980bb8" />
<img width="1920" height="1080" alt="Screenshot (249)" src="https://github.com/user-attachments/assets/f9fad2c9-4988-4f7f-b226-c3c31c5ba689" />
<img width="1920" height="1080" alt="Screenshot (252)" src="https://github.com/user-attachments/assets/e35534b5-fe14-4459-b3d0-3843bcd0823c" />
<img width="1920" height="1080" alt="Screenshot (258)" src="https://github.com/user-attachments/assets/d16d86e6-8c37-4953-97c6-68e3c723b5fc" />














