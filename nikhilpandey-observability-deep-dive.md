# 1. Observability Fundamental

- **Observability** means to see or look what's going on in our system by looking at the output and understanding our system.

## Pillars in Observability

- There are three pillars: **Metrics**, **Logs**, **Traces**.

### Metrics

- It's a number that changes with time.
- It is always a number.
- Recorded at specific moments.
- **Example (E-commerce):**
    - Number of Order per hour: 10M
    - Number of Cancelled Order: 1M
    - Number of Return/Damage Product: 500K
- **Example (Web Application):**
    - Request Per second: 1234
    - Response time: 100 ms
    - Memory used: 2.5 GB out of 8GB
    - Error count: 50

#### The Four Types of Metric

1.  **Type 1: Counter**
    - Only goes up. Never goes down (like an odometer in a car).
    - **Example:** Total pizza delivered (all-time) = 10M
        - Day 1: 100
        - Day 2: 2000... so on
    - **Why to use:**
        - To Track total occurrences (total profit, sales, error).
        - Calculates rates (divide by time: $10\text{k}/1\text{hr}$).
    - **Format:**
        ```
        Metric Name: http_req_total
        Labels: {method="Post", status="200"}
        Value: 123456
        ```

2.  **Type 2: Gauge**
    - Used when things can go up or down.
    - **Example:** Current Memory Usage
        - 10 AM: 10 GB
        - 10:10 AM: 2 GB
    - **Why to use:**
        - Trigger alerts ("if memory > 60%").
        - Show current state ("right now we have X capacity").
    - **Format:**
        ```
        Metric Name: Memory_Usage
        Label: {type:"server1"}
        Value: 2.5GB
        timestamp: 10AM
        ```

3.  **Type 3: Histogram**
    - Groups value into bucket or range to show data distribution.
    - Used when you want the whole distribution, not just the average.
    - **Example: API Response Time (ms)**
        - 0-10 ms: 100 requests (very fast)
        - 10-20 ms: 300 requests (normal)
        - 1000 ms: 20 request (timeout)
    - **Format:**
        ```
        Metric Name: http_request
        Labels: {method="GET", lessthanOREqual="0.05"} -> Value: 600 (600 request under 50ms)
        Labels: {method="GET", lessthanOREqual="0.1"} -> Value: 600 (600 request under 1000ms)
        ```

4.  **Type 4: Summary**
    - Like histogram but show data in percentile.
    - Tells you "X% of value below this point".
    - **Example:** 50 people take exam
        - 50 percentile: score 75 (50% score below 75, 50% scored above 75)
    - **Format:**
        ```
        http_requests_total Total number of HTTP requests
        TYPE http_requests_total counter
        http_requests_total{method="GET",status="200"} 1234567
        http_requests_total{method="POST",status="201"} 98765
        ```

#### Metric Data Type Format (How data are stored/transmitted)

- **Format 1: Prometheus Text Format (Human Readable)**
    - HTTP counter metrics: GET requests with status 200: 1,234,567 total
    - Memory gauge metrics: Server 01 using 2.5 GB
    - Request duration histogram: 100 requests took 0-10ms

- **Format 2: OLTP (OpenTelemetry Protocols) JSON**
    - Standard way to send observability data as it is well structured, works with any tool, and is self-describing.
    - **Example:**
        ```json
        {
        "name": "payment.amount",
        "description": "Total payment amount in dollars",
        "unit": "USD"
        }
        ```

### Label and Cardinality

- **Label:** Additional info attached to metric to split data.
    - **Without labels:** `http_request_total = 1M`
    - **With Labels Example:** `http_request{method="GET",status=200} = 900`

- **Cardinality:** The number of unique combination of labels values.
    - **Example:**
        - `method_value`: GET, PUT, POST, DELETE (4 values)
        - `status`: 200, 300, 404, 500 (4 values)
        - Combination = $4 \times 4 = 16$ unique time series.
        - **Cardinality: 16**

- **Why cardinality matters:**
    - Disk space
    - CPU for writes
    - Memory for index
    - **More cardinality = More resource required**

- **Solution:**

| KEEP IN METRICS (Low Cardinality) | MOVE TO LOGS (High Cardinality OK) |
| :--- | :--- |
| method: GET, POST, PUT, DELETE (4 values) | user_id: "user123" |
| status: 200, 404, 500 (5 values) | request_id: "req_xyz" |
| service: api, web, cache (3 values) | ip_address: "1.2.3.4" |
| region: us-east, us-west, eu (3 values) | session_id: "sess_abc" |

- **Why:** Metrics are Storage optimized (use 20 series, not 20 million). Logs can have millions of unique values.

---

## Pillar 2: Logs

- Defined in a text message as **what are happening** and **when it happen**.
- **Example: Patient Log**
    - `2024-11-19 09:00:15 - Patient arrived at emergency room`
    - `2024-11-19 09:05:30 - Blood pressure measured: 120/80`

### Type of Logs

- Unstructured Log
- Structured Log

#### Type 1: Unstructured Log

- Just Text, No format.
- **Example:**
    - `2024-11-19 10:30:18 ERROR Database connection failed`
- **Problem with unstructured logs:**
    1.  **HARD TO SEARCH:** Have to read through ALL logs and count manually.
    2.  **HARD TO PARSE:** Cannot easily extract error codes or specific data.

#### Type 2: Structured Log

- Organized data in JSON format.
- **Example:**
    ```json
    {
    "timestamp": "2024-11-19T10:30:18.123Z",
    "level": "ERROR",
    "message": "Database connection failed",
    "service": "user-api"
    }
    ```

---

## Pillar 3: Traces

- Shows the complete one path of a request from beginning to ending.
- **Example:** Customer walk in (10AM) -> Waiter given seat to customer (10:5AM)...

### Trace Terminology

1.  **TRACE:**
    - The complete journey of one request.
    - Has a unique **Trace ID**.
    - Contains many spans.

2.  **SPAN:**
    - One operation/step within a trace.
    - Has a unique **Span ID**.
    - Has a parent span (except root span).
    - Has start and end time, duration.
    - Has attributes (context), Has events (what happened).
    - May have errors.

3.  **Baggage:**
    - Data passed through the entire trace.
    - **Example:** In root span, set baggage: `user_id = "user_123"`, `request_id = "req_abc"`.
    - **Benefit:** Each span knows the context (user, request, experiment).

- **Use trace when:**
    - Debugging why a request is slow.
    - Understanding request flow across services.
    - Finding bottlenecks in microservices.
    - Investigating errors (which service failed?).
    - Performance analysis.

---

## How Three Pillars Work Together

**Scenario: Website is slow**

1.  **Metrics Alert:** `response_time_p95 > 1000ms` (Dashboard shows spike).
2.  **Trace Investigation:** Find trace ID of slow request, see payment service took 800ms.
3.  **Log Analysis:** Search logs with `trace_id`, find "Database connection pool exhausted".
4.  **Root Cause:** Database needs more connections!

**The Flow:**
Metrics (What's wrong?) $\downarrow$ Traces (Where's the problem?) $\downarrow$ Logs (Why did it happen?)

---

## Time-Series Nature of Observability Data

- Data points indexed by time, like stock prices or temperature readings.
- **Why Time-Series Databases?**
    - Regular databases (MySQL, PostgreSQL) are not optimized.
- **Specialized Time-Series Databases:**
    - InfluxDB - Push-based, flexible schema.
    - M3DB - Uber's distributed TSDB.
    - VictoriaMetrics - Fast and efficient.
---

# 2. 🤯 Data Collection Architecture: The Full Breakdown

This is all about how we actually get the data (metrics, logs, traces) from our apps and servers and clean it up before we store it. It's the messy middle part that makes observability actually work and not cost a million bucks.

---

## 🛠️ What the Heck is a Collector? (Why it's a Must-Have)

A **Collector** (like the **OTel Collector**) is basically a traffic cop/cleaner station for all our observability data. It sits between our running applications and the final storage place (the backend).

### Why I Can't Just Skip It and Go Direct

Trying to send data straight from the app to the storage backend is asking for trouble.

1.  **Data Loss Protection (The Buffer):**
    -   *Real Problem:* Say the database holding our logs goes down for 5 minutes. If my app tries to send logs directly, those 5 minutes of data are just *gone* forever because the app won't wait.
    -   *Collector's Job:* The collector catches that data, holds it temporarily (it **buffers** it), and keeps trying to send it later, even if the backend is down. It guarantees data delivery, which is huge!
    -   *Super Detail:* It uses smart retry rules (like waiting longer and longer between tries) so it doesn't just spam the down backend.

2.  **Cost Control and Cleanup (The Filter):**
    -   *Real Problem:* Sometimes a tiny code bug causes an app to spew thousands of useless log messages a second ("log bomb"). If those hit the backend, my cloud bill goes sky-high instantly.
    -   *Collector's Job:* I can tell the collector, "Hey, if the log says 'DEBUG' or 'Status 200/OK', just throw it away." This **filtering** happens *before* I pay to store it. Massive money saver.

3.  **Application Offload (The Heavy Lifter):**
    -   *Real Problem:* Sending data securely means setting up HTTPS connections, encrypting it, and managing retries. That takes CPU and RAM away from my main application, making the customer experience slower.
    -   *Collector's Job:* My app only talks to the local collector (super fast, efficient). The collector handles all the slow, hard work of network encryption and dealing with the remote backend. My app stays fast!

---

## 🧭 Collection Models: How We Get Our Hands on the Data

There are three main ways to grab the data from the source machine.

### 1. Agent-Based (The Roommate Method)

-   **Idea:** Put a small, dedicated program (**the agent**) right on the same machine/server/VM as the application.
-   **How it works:** The agent lives locally and watches everything.
-   **Pros (Why it's good):**
    -   **Reliable:** Super short distance for data to travel.
    -   **Deep Access:** It can easily read local log files and talk to the kernel to get system stats (CPU, memory) without network issues.
-   **Cons (The downside):**
    -   **Resource Use:** The agent takes up a bit of CPU and RAM on every single machine—I have to plan for that.
    -   **Maintenance Hell:** If I have 1,000 servers, I have 1,000 agents to manage and update. Ugh.

### 2. Agentless (The Remote Scraper Method)

-   **Idea:** Don't install anything locally. The central collector reaches out over the network to grab the data.
-   **How it works:** This is common for metrics. The application exposes an endpoint (`/metrics`), and the remote collector connects periodically and **scrapes** the numbers.
-   **Pros (Why it's good):**
    -   **Easy Deployment:** I deploy fewer collectors overall.
    -   **Zero Overhead:** The application host loses zero resources to the collector process.
-   **Cons (The downside):**
    -   **Network Sensitive:** If the network gets glitchy, my data collection gets gaps.
    -   **Shallow View:** It only gets the data the application *chooses* to expose over that endpoint. Hard to get low-level system details.

### 3. eBPF-based (The Kernel Spy Method)

-   **Idea:** This is next-level tech. It uses **eBPF** to run tiny, safe programs right inside the heart of the Linux Operating System (the **Kernel**).
-   **Why it's crazy different:**
    -   It sees things without touching the code. I don't need any instrumentation!
    -   It captures data at the **lowest layer** (system calls, network packets).
-   **Killer Advantages (Why we want it):**
    -   **Invisible Tracing:** It can automatically trace network calls and I/O latency for *any* process running, even if I don't have the source code.
    -   **Super Fast:** It runs so efficiently in the kernel; the overhead is tiny.
    -   **Deepest Root Cause:** It can tell me exactly which process caused a certain network error, information a user-level agent can't see easily.

---

## 🤝 Push vs. Pull: Who Starts the Conversation?

This decides whether the application pushes the data or the collector pulls it.

### 1. Push Model (App Sends Data)

| Why Use It? | When Data is **Event-Driven** | Performance Detail |
| :--- | :--- | :--- |
| **Logs & Traces:** An error or event happens *now*. The data must be sent immediately. | **Low Latency:** Data arrives instantly, perfect for real-time alerts. | **The Problem:** Backend gets hit with massive spikes when lots of errors happen at once. |

### 2. Pull Model (Collector Requests Data)

| Why Use It? | When Data is **Time-Series** | Performance Detail |
| :--- | :--- | :--- |
| **Metrics:** It's okay if the number is 15 seconds old. The collector checks the app every 15s. | **Control:** The collector sets the frequency, which makes traffic patterns predictable. | **The Problem:** I won't know about a crash until the next scheduled scrape happens. |

---

## 🏭 Processing at the Collector: The Cleanup Crew

The collector is a factory! It takes raw, dirty data and turns it into clean, useful data. This is where we save money and make our data actually queryable.

### 1. Filtering Unwanted Data

-   **Why I do it:** To throw away the trash instantly.
-   **Detail:** I set up rules: "Drop every log entry from the `/dev/health` endpoint," or "If the metric label is `status=200`, drop 90% of them."
-   **Impact:** Saves huge money on ingestion and makes my backend queries faster because it has less noise to scan.

### 2. Sampling Strategies (Making Traces Affordable)

-   **Why I do it:** Traces are the most expensive data type. I can't keep every single trace, so I only keep the most interesting ones.
-   **Head-Based Sampling:**
    -   **Mechanism:** Decide to keep the trace at the **very start** (the head).
    -   *The risk:* I might drop a trace that runs fine for 5 services but then hits a timeout on the last service. I dropped the important one!
-   **Tail-Based Sampling:**
    -   **Mechanism:** The collector waits until the **whole trace** is complete, then decides if it was interesting (e.g., did it error out? was it too slow?).
    -   *The benefit:* I guarantee I keep the high-value, slow, or broken traces.
    -   *The cost:* The collector needs a lot of memory to hold all those partial traces temporarily. This makes the collector itself resource-intensive.

### 3. Buffering and Batching (Network Optimization)

-   **Buffering:** Holding data when the network or backend is congested. (Covered this, it's the reliability part.)
-   **Batching:** Instead of sending 100 small messages, the collector waits 100ms, bundles them into one big package, and sends that.
-   **Impact:** Massive reduction in network traffic and fewer connections/transactions needed, saving CPU on both ends.

### 4. Metadata Enrichment (Adding the Context)

-   **Why I do it:** My application code doesn't know its own IP address, Kubernetes pod name, or region ID. That info is needed for analysis.
-   **The Process:** The collector automatically looks up its environment and stamps that information onto the log/metric/span.
-   **Example:** A log just says "Connection refused." The collector adds `k8s.namespace=payments`, `region=us-west-2`, and `service.version=v3.1`. Now I know exactly *where* and *when* the problem happened.

### 5. Protocol Translation (Making Everyone Speak OTLP)

-   **Why I do it:** I have old tools sending weird StatsD metrics. My new storage backend only accepts the modern **OTLP** format.
-   **The Process:** The collector has an internal converter. It takes the old format in and spits the standard OTLP format out, unifying my data stream.

---

## 🔬 Collection at Different Layers (The Full Stack)

We need data from every part of the system, not just the code.

| Layer | Where the Data Comes From | Why It Matters | Collection Method |
| :--- | :--- | :--- | :--- |
| **Application Layer** | My Python/Java code, the business logic. | Tells me about user experience and features (e.g., checkout latency). | Manual SDK calls or Auto-instrumentation. |
| **System Layer** | The server's Operating System (OS). | Tells me if the problem is a resource issue (e.g., CPU is pegged at 100% or memory is full). | Local agent watching OS files/APIs. |
| **Network Layer** | Data flowing in and out of the server's network card. | Tells me if the problem is outside my server (e.g., high latency to a third-party API). | Agent or eBPF (super detailed). |
| **Kernel Layer** | The deepest, lowest level of the OS. | Tells me about process scheduling delays or file system I/O issues (the true root cause). | Mostly eBPF. |

---

## 🧑‍💻 Auto- vs. Manual-Instrumentation

Instrumentation is how we turn normal code into code that generates traces and metrics.

### Manual Instrumentation (The Hard Way)
-   **How:** I manually write code like `span = tracer.start_span("process_order")`.
-   **Pros:** I get to track *exactly* what I want (custom logic). Best fidelity.
-   **Cons:** Takes forever! I have to write and maintain tons of observability code.

### Auto-Instrumentation (The Easy Way)
-   **How:** I start my app with an agent (e.g., Java Agent). It magically injects itself into framework calls.
-   **Pros:** Instant tracing for standard stuff (HTTP requests, database calls) with zero code changes.
-   **Cons:** It's generic. It doesn't know my custom business logic (e.g., it knows I called a database, but not *why*).

---

## 📈 The Final Flow (Putting It All Together)


The entire architecture is designed to be **reliable, affordable, and smart**. The data goes through these steps:

1.  **Generate:** Apps and hosts make data.
2.  **Ingest:** Local Agent or remote Pull gets the data to the Collector.
3.  **Process:** The Collector **filters** the trash, **batches** the good stuff, **samples** the expensive traces, and **enriches** everything with context tags.
4.  **Export:** The Collector sends the clean, minimized data to the right specialized storage backend (one for metrics, one for logs, one for traces).
5.  **Analyze:** I finally query the clean data to find out what broke!
---

# Backend Pipeline Architecture

## The Big Picture
When data flows in from collectors, it enters the backend pipeline — the part where the real heavy lifting happens.  
Raw data arrives hot and messy, then gets processed, organized, stored, and finally made queryable.

Think of it like a factory:
- **Raw materials** → data ingestion  
- **Assembly line** → processing stages  
- **Packaging & storage** → databases / storage systems  
- **Shipping** → query layer serving data to users or services

## End-to-End Data Flow  
Here's the journey:

text
┌──────────────────────────────────────────────────────────────────────┐
│ DATA SOURCES                                                         │
│ (Collectors, Agents, Apps sending data)                             │
└──────────────────────────────────────────────────────────────────────┘
                              ⬇️
┌──────────────────────────────────────────────────────────────────────┐
│ INGESTION LAYER                                                      │
│ (HTTP, gRPC, TCP - How data arrives)                                │
│ - Load Balancing                                                     │
│ - Initial Validation                                                 │
│ - Routing                                                            │
└──────────────────────────────────────────────────────────────────────┘
                              ⬇️
┌──────────────────────────────────────────────────────────────────────┐
│ PROCESSING STAGES                                                    │
│ - Parsing & Normalization                                           │
│ - Aggregation (rates, percentiles)                                  │
│ - Rollups                                                            │
│ - Indexing                                                           │
│ - Enrichment & Transformation                                       │
└──────────────────────────────────────────────────────────────────────┘
                              ⬇️
┌──────────────────────────────────────────────────────────────────────┐
│ STORAGE LAYER                                                        │
│ (Different databases for different data types)                      │
│ - Metrics: Prometheus, InfluxDB, M3DB, VictoriaMetrics, ClickHouse │
│ - Logs: Elasticsearch, Loki, ClickHouse                            │
│ - Traces: Jaeger, Tempo                                            │
└──────────────────────────────────────────────────────────────────────┘
                              ⬇️
┌──────────────────────────────────────────────────────────────────────┐
│ QUERY LAYER                                                          │
│ (Users query the data)                                              │
│ - Query Languages: PromQL, LogQL, TraceQL, SQL                     │
│ - Query Optimization                                                │
│ - Caching                                                           │
└──────────────────────────────────────────────────────────────────────┘
## Ingestion Layer: How Data Arrives

### Protocols: HTTP, gRPC, TCP
Data can arrive through multiple protocols — each with pros, cons, and ideal use cases.

---

### **HTTP (REST style)**
- Easy, familiar, debuggable  
- Higher overhead (headers, connection setup)  
- Supports both pull and push models  
- **Example:** Pushing metrics to a webhook endpoint

---

### **gRPC (Google's RPC)**
- Binary format → smaller & faster  
- Multiplexing (multiple requests over a single connection)  
- Lower latency and bandwidth usage  
- **Example:** OpenTelemetry uses gRPC for collector communication

---

### **TCP (Raw sockets)**
- Extremely fast  
- Zero HTTP overhead  
- Harder to debug & manage  
- **Example:** Some log shippers send logs via raw TCP streams

---

### **Which one should you use?**
- **High volume, latency-critical:** gRPC  
- **Simple setup & easy debugging:** HTTP  
- **Extreme throughput & performance:** TCP (but more complex)

---

## Load Balancing and High Availability
You can’t rely on a single ingestion endpoint—if it goes down, you lose data.

**Load balancing strategy:**

text
Collector 1 ──\
              → Load Balancer → Backend Pool 1
Collector 2 ──/                     ↓
              \                  Store Data
Collector 3 ──→ Load Balancer → Backend Pool 2
                                     ↓
                                More scaling

The load balancer (e.g., Nginx, HAProxy) sits in front and distributes incoming traffic.  
If one backend instance goes down, traffic is automatically redirected to healthy ones.

### High Availability Includes:
- **Multiple ingestion nodes** (redundancy)
- **Replication** (write data to multiple places)
- **Queue buffering** (prevents data loss if backend becomes slow)

---

## Initial Validation and Routing

### Validation
Not all incoming data is valid. The first step: validate early.

Checks include:
- Is the JSON/protocol format valid?
- Are required fields present?
- Is the timestamp within a reasonable range?

Bad data should be **rejected or dropped** early.

### Routing
After validation, decide **where the data goes**.

Examples:
- Metrics → time-series database
- Logs → log storage
- Traces → trace backend

Routing is based on data type or routing rules.

---

## Processing Stages: Where The Work Happens

### Parsing and Normalization

Raw data comes in many formats:
- Plain text
- StatsD: `requests:100|c`
- Prometheus: `requests_total 100`
- JSON: `{"metric": "requests", "value": 100}`
- CSV: `"requests,100"`

Parsing converts each format to a **standard internal format**.  
Normalization ensures consistency:
- Unified timestamp format  
- Clean metric names  
- Standardized labels  

After this, all data is uniform for the rest of the pipeline.

---

### Aggregation

Aggregation computes summaries across multiple data points.

Common examples:
- **Rate:** requests per second  
- **Percentiles:** p50 / p95 / p99 latency  
- **Sum:** total requests across servers  
- **Average:** mean CPU usage  

#### Why Aggregate?
- Raw data is too noisy
- Saves storage massively
- Users care about trends, not every data point

#### Real Example:
1000 servers sending CPU metrics → 100,000 data points/minute.  
Aggregated to 1 value/minute = huge savings.

---

### Rollups

Rollups store older data with **reduced resolution**.

| Time Range        | Granularity               |
|-------------------|---------------------------|
| 0–1 hour          | Full resolution           |
| 1–24 hours        | 5-minute aggregations     |
| 1–30 days         | 1-hour aggregations       |
| 30+ days          | 1-day summaries           |

#### Benefits:
- Detailed recent data → useful for debugging  
- Older data → cheaper, summarized  
- 90%+ storage savings for long-term data

---

### Indexing

Indexes make data fast to find.

Example:
Query logs for `status=500` and `env=prod`.  
Without index → scan 1B records.  
With index → lookup 10k records instantly.

Indexing types:
- Time-based indexes  
- Label/tag indexes  
- Full-text indexes (for logs)

---

### Enrichment and Transformation

Adds missing context to raw data.

#### Enrichment Examples:
- Add `datacenter=us-west-2` from IP
- Add `team=backend` from service name
- Add `severity=critical` from error type

#### Transformation Examples:
- Bytes → MB  
- ms → seconds  

Makes queries and dashboards more meaningful.

---

## Storage Layer: Where Data Lives

Different data = different storage technologies.

---

## Time-Series Databases (Metrics)

### Prometheus
- Pull-based
- Simple, reliable
- Limited retention, struggles with high cardinality

### InfluxDB
- Push-based
- Good at high cardinality
- Cluster support (paid)

### ClickHouse
- Columnar database
- Extremely fast analytics
- Great for huge scale, but complex

### M3DB
- Built for massive scale
- Distributed
- Automatic aggregation

### VictoriaMetrics
- Simple deployment
- High cardinality support
- Scales horizontally

#### Comparison:
- **Small scale:** Prometheus  
- **High volume / cardinality:** InfluxDB, VictoriaMetrics  
- **Massive scale:** ClickHouse, M3DB  
- **Simple:** Prometheus, VictoriaMetrics  

---

## Log Storage

### Elasticsearch
- Full-text search
- Highly scalable
- Expensive to run

### Loki
- Label-based search
- Lower cost
- Not full-text

### ClickHouse
- Fast  
- Stores logs as structured data  

#### Comparison:
- Need full-text: **Elasticsearch**
- Cost-effective: **Loki**
- Massive scale: **ClickHouse**

---

## Trace Storage

### Jaeger
- Search by tags, duration, metadata  
- Uses Cassandra/Elasticsearch/etc.

### Tempo
- Uses object storage → very cheap  
- Can't search by arbitrary tags (needs trace ID)

#### Comparison:
- Full search: Jaeger  
- Low cost: Tempo  

---

## Why Different Storage?

### Metrics
- High volume  
- Numerical  
- Time-based  

### Logs
- Text-based  
- Irregular  
- Search-heavy  

### Traces
- Hierarchical spans  
- Service-to-service correlation  

---

## Hot vs Warm vs Cold Storage

### Hot (0–7 days)
- Full resolution
- SSD/NVMe
- Fast but expensive

### Warm (8–30 days)
- Aggregated
- HDD
- Medium cost

### Cold (30+ days)
- Highly aggregated
- Archive storage
- Very cheap, slow

---

## Compression Techniques

### Delta Encoding
- Store differences, not raw values

### Run-Length Encoding
- Store repeated values as (value × count)

### LZ4 / Snappy
- General-purpose fast compression

### Columnar Storage
- Column-by-column storage (ClickHouse, Parquet)

Combined savings: **10–100x reduction**

---

## Retention Policies

Example:
- 1–7 days: full resolution  
- 8–30 days: 5-min aggregates  
- 1–90 days: hourly  
- 90+ days: delete  

Most companies:
- Detailed: 7–15 days  
- Aggregated: 30–90 days  
- Summaries: 1 year  

---

## Query Layer: Making Data Accessible

### Query Languages:
- **PromQL** → metrics  
- **LogQL** → logs  
- **TraceQL** → traces  
- **SQL** → ClickHouse, analytics  

---

## Query Optimization

### Problem: High cardinality  
Use specific label matchers.

### Problem: Regex matching  
Use exact match when possible.

### Problem: Large result sets  
Aggregate early.

### Problem: Long time ranges  
Use coarser intervals.

---

## Aggregation at Query vs Storage Time

### Query-Time:
- Flexible  
- Slower  

### Storage-Time:
- Fast queries  
- Less flexible  

### Hybrid:
Most systems do both.

---

## Caching Strategies
- In-memory caching  
- Block caching  
- Query result caching  
- TTL-based expiration  

---

## Backend Architecture Comparison

### DataDog (SaaS — All-in-one)

**Pros**
- Easy to set up
- Real-time alerting
- Great integrations
- Great UI

**Cons**
- Very expensive
- Vendor lock-in

---

### Grafana Stack (Open Source)

Components:
- Prometheus  
- Loki  
- Tempo  
- Grafana  

**Pros**
- Cheap
- Open-source
- Customizable

**Cons**
- More ops work
- Requires expertise

---

## Side-by-Side Comparison

| Feature | DataDog | Grafana Stack |
|--------|---------|----------------|
| Setup Time | Days | Weeks |
| Op Burden | Low | High |
| Cost Small | $$$ | $ |
| Cost Large | $$$$ | $ |
| Customization | Low | High |
| Integrations | 400+ | 100+ |
| Anomaly Detection | Excellent | Average |
| Learning Curve | Medium | High |
| Data Lock-in | Yes | No |

---

## Bottlenecks in Backend Pipelines

### Ingestion Bottleneck
Symptoms: backlog, delays  
Fix: scale horizontally, batch, sample data

### Processing Bottleneck
Symptoms: CPU maxed  
Fix: add nodes, parallelize, simplify logic

### Storage Write Bottleneck
Symptoms: queue buildup  
Fix: batch writes, compression, faster disks

### Storage Read Bottleneck
Symptoms: slow dashboards  
Fix: caching, indexing, reduce cardinality

### Query Bottleneck
Symptoms: one bad query slows system  
Fix: timeouts, rate limiting, user education

---

## Real-World Example: Payment Service Observability

### Setup:
- Collectors: OTel
- Ingestion: HTTP → DataDog / Prometheus
- Processing:  
  - Parse OTel format  
  - Aggregate p50/p95/p99  
  - Enrich with region, version  
- Storage:  
  - Metrics → TimescaleDB  
  - Logs → Elasticsearch  
  - Traces → Tempo  
- Query:  
  - PromQL (metrics)  
  - Alerts (error rate > 1%)  
  - TraceQL (slow transactions)

### Why this works:
- High throughput (1000 req/sec)
- Low latency  
- Complete visibility  
- Cost efficient  

---

# Intelligence Layer: Insights, Anomalies, and RCA

## A. Insights: Understanding Your Data

Alright, so you got data. Mountains of it. But data is useless without understanding what it means. That's where insights come in.

### What Even Are Insights?

An insight is a meaningful pattern extracted from raw data that tells you something actionable.

- **Raw data:** "latency = 500ms"  
  **Insight:** "API latency increased 50% in the last hour compared to average"

- **Raw data:** "errors: 10"  
  **Insight:** "Error rate spiked on payment-service during deployment"

Insights answer the question:  
**"What's happening in my system that I should care about?"**

### How Are Insights Generated?

Observability tools process raw data through several techniques:

text
Raw Data (millions of points)
        ↓
   Data Aggregation
        ↓
   Statistical Analysis
        ↓
   Pattern Recognition
        ↓
   Correlation
        ↓
   Insight (actionable knowledge)
   
# Pattern Recognition Techniques

## Statistical Analysis (The Math Part)

### Mean (Average)

latencies = [100ms, 120ms, 110ms, 150ms]  
mean = (100 + 120 + 110 + 150) / 4 = 120ms  
Good for: Quick overview, but hides outliers

### Median (Middle Value)

latencies = [100ms, 120ms, 110ms, 500ms]  # sorted  
median = (110ms + 120ms) / 2 = 115ms  
Why: Ignores extreme outliers (like that 500ms spike)  
Better than mean for real data

### Percentiles (Position in Distribution)

p50 (50th percentile / median) = 115ms  
p95 (95% of requests below) = 400ms  
p99 (99% of requests below) = 480ms  

Real world: "95% of requests complete in under 400ms"  
This tells you: most users are happy, but 5% get slow responses

### Standard Deviation (How Much Data Varies)

latencies = [100, 120, 110, 150, 200]  
mean = 136ms  
std_dev = 40ms (roughly)

If std_dev is low: data is consistent  
If std_dev is high: data varies wildly

---

## Trend Analysis

Looking at data over time to spot directions.

Example:

Week 1: CPU = 40%  
Week 2: CPU = 45%  
Week 3: CPU = 52%  
Week 4: CPU = 60%

Insight: "CPU usage trending upward, will hit 100% in ~8 weeks"

Simple method: Compare old average to new average

Old avg (last month): 40%  
New avg (last week): 55%  

Trend: +37% increase  

This triggers: "Need to scale up or optimize"

---

## Correlation Analysis

Finding which things change together.

### Example 1:

When API response time increases → error rate also increases  
Correlation = Strong positive  
Insight: "Slow API is correlated with errors (maybe timeout?)"

### Example 2:

When we deploy new code → latency spikes  
Correlation = Strong positive (timing)  
Insight: "Recent deployment caused latency"

Temporal correlation: "Did X happen before Y?"  
Causal correlation: "Did X cause Y?"

Real correlation detection:

- Collect multiple metrics  
- Calculate correlation coefficient (-1 to +1)  
- If close to +1 or -1 → strong correlation  
- Alert on strong unexpected correlations

---

## Machine Learning Approaches

Simple statistics are good, but ML can do more.

---

### Clustering (Grouping Similar Patterns)

What: Group similar time-series or events together.

Example:

Normal traffic pattern (daytime) → Cluster 1  
Normal traffic pattern (nighttime) → Cluster 2  
Unusual spike → Outlier

Use case: "Detect when traffic pattern is unusual"

Without clustering: Hard to define 'normal'  
With clustering: System learns what normal looks like

---

### Forecasting (Predicting the Future)

What: Train a model on past data to predict future values.

Example:

Historical data: Traffic at 2 PM ≈ 500 req/sec  
Forecast for tomorrow 2 PM: 500 req/sec  
Actual: 1000 req/sec  

Alert: "2x more traffic than expected"

Why it's better than thresholds:

Threshold: "Alert if traffic > 800 req/sec"  
Forecast: "Alert if traffic > forecast + 20%"

---

### Classification (Labeling Events)

What: Categorize events automatically.

Example:

Error log: "Connection timeout" → Network Error  
Error log: "Out of memory" → Resource Error  
Error log: "Invalid JSON" → Application Error  

Then group by type:

Network errors: 30%  
Resource errors: 50%  
Application errors: 20%  

Insight: "Main problem is resource errors, need to scale"

---

## Real-World Insight Examples

### Example 1: "API latency increased 50%"

Tool collected:

- API response time: 100ms → 150ms  
- CPU usage: 40% → 60%  
- Database query time: 20ms → 60ms  
- Cache hit rate: 90% → 60%

Insight generated:  
"API latency spike correlates with low cache hit rate and high DB latency.  
Likely cause: Cache miss storm due to data expiration."

Action: Investigate cache settings, increase TTL.

---

### Example 2: "Error rate spike on service X"

Tool collected:

- Error rate: 0.1% → 5%  
- Service latency: 50ms → 2000ms  
- Recent deployment: Yes  
- Dependency availability: All fine

Insight generated:  
"Error spike correlates with recent deployment of service X.  
High latency suggests slow handler or resource contention."

Action: Rollback deployment or check new code.

---

# B. Anomalies: Detecting The Weird Stuff

An anomaly is data that doesn't match expected patterns.

---

## What Is an Anomaly?

Examples:

- Spike: CPU 40% → 95%  
- Dip: Traffic 1000 → 100 req/sec  
- Trend shift: Latency 100ms → 500ms  
- Seasonal violation  
- Pattern violation

---

## Threshold-Based Detection

### Static threshold

Rule: Alert if error rate > 5%  
Problems: False positives or misses issues.

### Dynamic threshold

Calculate baseline → Compare with today → Alert only if deviation is big.

Best for: Stable systems

---

## Statistical Methods

### Z-Score

z = (value - mean) / std_dev

If z > 3 → anomaly

### IQR

Outlier if value outside [Q1 - 1.5×IQR, Q3 + 1.5×IQR]

Good for: Non-normal data

---

## Machine Learning Methods

### Isolation Forest

Anomalies are isolated quickly in decision trees.

Pros:

- Fast  
- High-dimensional  
- No distribution assumptions  

Cons:

- Hard to explain  
- Needs tuning  

### LSTM Autoencoders

Train on normal data → Reconstruct → High error = anomaly

Pros:

- Accurate  
- Learns seasonality  

Cons:

- Needs lots of data  
- Expensive  

---

## Handling Seasonality and Trends

Solutions:

- Seasonal decomposition  
- Compare to same time last week  
- Use baselines  

---

## False Positive Reduction

- Severity classification  
- Duration-based alerts  
- Correlation-based alerts  

Goal: < 5% false positives

---

# C. Root Cause Analysis (RCA)

Anomaly = something is wrong  
RCA = why it's wrong

---

## Why RCA Is Hard

Distributed systems have:

- Many dependencies  
- Many data sources  
- Timing complexity  
- Hard-to-see relationships  

---

## RCA Techniques

### 1. Correlation Across Metrics

Find what changed together near anomaly timestamp.

### 2. Dependency Mapping

Trace through service graph to locate failing dependency.

### 3. Trace Analysis

Find bottleneck in trace:

Payment Service  
→ Database call slow  
→ Root cause: DB

### 4. Log Aggregation

Search logs around anomaly:

Database timeout errors → Root cause

### 5. Change Event Correlation

Recent deployment? Config change? Migration?

---

## RCA Example (Full)

14:05 → Latency alert  

Metrics: CPU ↑, DB pool ↓, cache hit rate ↓  
Traces: DB query slow  
Logs: Connection pool exhausted  
Change: Deployment at 14:00 with new query  

Root cause: Unoptimized DB query in new release

Action: Rollback, fix query

---

# How Observability Tools Help

## Unified View

Metrics + Logs + Traces together = fast RCA

## Automatic Correlation

Tools detect correlated patterns automatically.

## AI-Powered RCA

Uses dependency graph + causal inference.

---

# Real Tools and Workflows

## DataDog

- Click alert  
- View topology  
- See correlated logs/traces  
- Identify root cause  

## Grafana Stack

Manual correlation via Prometheus + Loki + Jaeger.

---

# Summary

Raw Data  
→ Insights  
→ Anomalies  
→ RCA  
→ Action

That's the intelligence layer.

---

# Observability User Experience

## A. Dashboard Patterns

### Common Visualization Types

#### Time-Series Graphs (Line, Area, Stacked)

These show data over time. You plot time on the X-axis, value on Y-axis.

- **Line graph:** Single line shows one metric over time. Example: CPU usage from 9 AM to 5 PM  
- **Area graph:** Same as line but filled below. Makes it easier to spot trends  
- **Stacked area:** Multiple metrics stacked on top. Example: breakdown of request types (GET, POST, DELETE)

**Best for:** Spotting trends, seeing changes  
**Real example:** "Error rate over last 24 hours" — line graph makes 2 PM spike obvious

---

#### Gauges and Single-Stat Panels

Big number showing ONE value.

- **Gauge:** Like a speedometer. CPU: 75%  
- **Single stat:** Just one number. "Errors last hour: 523"

---

#### Heatmaps (Latency Distribution)

Dark = more frequent, Light = rare.

```
Latency (ms)     Frequency
0-100            ████████████
100-200          ███
200-300          ██
300+             █
```

---

#### Histograms

```
Latency Buckets:
0-50ms:    ████████ (80)
50-100ms:  ███ (30)
100-200ms: ██ (20)
200+ms:    █ (5)
```

---

#### Tables & Logs

**Table Example:**

```
| Service | CPU | Memory | Status |
|---------|-----|--------|--------|
| api-1   | 45% | 2.1GB  | Healthy |
| api-2   | 92% | 4.2GB  | Warning |
```

**Logs Example:**

```
2025-01-20 10:15:32 ERROR payment-service: Connection timeout
```

---

### Dashboard Organization

#### Infrastructure Dashboards


<img width="1920" height="1080" alt="Screenshot (216)" src="https://github.com/user-attachments/assets/54972089-8292-4414-8af3-fae5c5c7240a" />
<img width="1920" height="1080" alt="Screenshot (215)" src="https://github.com/user-attachments/assets/d706bda0-9d18-4404-9d39-94d5ed13c73a" />
<img width="1920" height="1080" alt="Screenshot (212)" src="https://github.com/user-attachments/assets/9a481b28-50da-4784-8018-e6ede9bf7583" />
<img width="1920" height="1080" alt="Screenshot (211)" src="https://github.com/user-attachments/assets/90698b24-a91f-4e24-9561-6e65909a9e69" />

---

#### Application Dashboards (RED Metrics)

```
Requests/sec: 1,250 | Error rate: 0.5% | p99: 145ms
```

#### Business KPI Dashboards

```
Revenue Today: $45,230
Signups: 234 (↑12%)
```

---

### Variables & Templating

```
Environment: [Prod ▼]
Service: [payment-api ▼]
Region: [us-west-2 ▼]
```

---

### Information Density

Good dashboard:

```
Summary: Rate | Errors | Latency
Row 1: Request rate graph
Row 2: Error breakdown
Row 3: Latency percentiles
Row 4: Slow endpoints table
```

---

## B. User Workflows

### Debugging an Incident

#### Step 1: Alert

```
Error rate: 8.3%
Service: payment-api
Started: 2:15 AM
```

#### Step 2: Identify Time & Components

```
2:00 AM: Deployment v2.3
2:15 AM: Error spike
```

#### Step 3: Drill Down to Instance

```
api-1: 2.1%
api-2: 8.9%  ← root issue
```

#### Step 4: Logs

```
ERROR: Database connection timeout after 30s
```

#### Step 5: Trace

```
payment-api
  ├─ Validate request: 2ms
  ├─ Auth: 15ms
  ├─ DB Query: TIMEOUT 30s
```

#### Step 6: Fix

Rollback. Errors drop.

---

### Metrics → Logs → Traces Flow

Good UX:

```
Click spike → Logs auto-filter → Click log → Opens trace
```

Bad UX:

```
Copy timestamp → open logs → paste → copy trace ID → open traces
```

---

### Time Sync

```
User selects: Last 1 hour
Metrics: 1h
Logs: 1h
Traces: 1h
```

---

### Search & Filtering

```
Service: [api]
Status: [error]
Host: [prod-2]
Time: [Last 24h]
```

---

### Drill Down

```
All services →
    api-service →
        api-2 →
            /api/users →
                Slow DB query
```

---

## C. Tool Analysis

### Grafana

**Strengths:** Flexible, beautiful, free  
**Weaknesses:** Manual setup, no built-in anomaly detection  

---

### DataDog

**Strengths:** Auto-correlation, great anomaly detection  
**Weaknesses:** Expensive, vendor lock-in  

---

### New Relic

**Strengths:** Strong APM, auto instrumentation  
**Weaknesses:** UI clutter  

---

## What Makes a Dashboard Intuitive?

Good:

```
Key stats → Trends → Breakdowns → Tables
```

Bad:

```
47 tiny graphs
No hierarchy
Can't read anything
```

---

## Handling Large Data

### Pagination

```
Page 1: rows 1-10
Page 2: rows 11-20
```

---

### Virtualization

Render only visible rows.

---

### Aggregation

```
Raw: 1,000,000 rows
Aggregated: Requests/min per endpoint
```

---

## Real-Time vs Static

Real-time: Updates every 1s  
Static: Updates every 5-10s  

---

## Insights / Anomalies / Alerts

Grafana: red lines  
DataDog: detailed insight cards  
New Relic: anomaly shading  

---

## What I'd Improve

- AI-powered RCA  
- Better filtering (natural language)  
- Faster drilldown  
- Better defaults  

---

## User Mental Model

```
"Something is broken"
   ├─ Where?
   ├─ When?
   ├─ How bad?
   ├─ Why?
   └─ How to fix?
```

Dashboard should answer these in order.

```
WHAT: Error spike
WHERE: payment-api
WHEN: 2:15–2:35 AM
WHY: Deployment event
HOW TO FIX: Rollback suggested
```


# Technical Challenges & Trade-offs in Observability

## Data Volume & Cost

### How Much Data Do Large Applications Generate?

Let's do some math.

### Small app (startup):
- **100 requests per second**
- Each request generates **10 metrics** (latency, status, endpoint, etc.)
- = **1,000 metrics per second**
- = **86 million metrics per day**
- **Storage:** ~1–2 GB per day (manageable)

### Medium app (scale‑up):
- **10,000 requests per second**
- Each request generates **10 metrics**
- = **100,000 metrics per second**
- = **8.6 billion metrics per day**
- **Storage:** ~100–200 GB per day (starting to get expensive)

### Large app (Netflix scale):
- **1,000,000 requests per second**
- Each request generates **10 metrics**
- = **10 million metrics per second**
- = **864 billion metrics per day**
- **Storage:** ~10–20 TB per day (VERY expensive)

### Also add:
- Logs: **10–100 TB/day**
- Traces: **1–5 TB/day**
- Total: **20–100 TB/day**
- Annual: **7–36 PB** (petabytes) per year — *not a typo*

---

## Storage Costs at Scale

### Cloud storage pricing (2025):
- **AWS S3 Standard:** ~$0.023 per GB/month  
- **AWS S3 Glacier (archive):** ~$0.004 per GB/month  

### Example: Netflix‑scale (20 TB/day)
- Monthly: 20 TB × 30 = **600 TB**
- In GB: 600 × 1024 = **614,400 GB**

- **S3 Standard cost:** 614,400 × 0.023 = ~$14,000/month  
- **S3 Glacier cost:** 614,400 × 0.004 = ~$2,400/month  

Annual (standard): **~$168,000**

But this is JUST storage — add read/query costs and total observability cost easily becomes **$250k–$500k/year**.

### Why companies pay this:
Because downtime is way more expensive.

Example: If Netflix goes down for 1 hour:
- Revenue lost: **Millions**
- Customer churn: **Huge**
- Brand damage: **Massive**

So spending $500k/year for observability = *cheap insurance*.

---

## Network Bandwidth for Data Transmission

20 TB/day:
- 20 TB = 20,000 GB = 20,480,000 MB  
- Per second: 20,480,000 / 86,400 ≈ **237 MB/sec**

This requires:
- Dedicated high‑speed network
- Redundancy
- Compression (ideally 10×)

### With compression:
- Without: **237 MB/sec**
- With 10× compression: **23 MB/sec**

Still huge but manageable.

---

# Cardinality Explosion

### Cardinality = number of unique label combinations.

Example metric:

```
http_requests_total{method="GET", status="200", service="api"}
```

Base example:
- 5 methods × 10 status codes × 3 services = **150** unique time‑series (OK)

But now add:
- 50 endpoints  
- 10 regions  
- 5 versions  
- 10,000 containers  

New cardinality:  
50 × 10 × 5 × 10,000 = **25,000,000** time‑series (BAD)

---

## Common Causes of Cardinality Explosion

### 1. Dynamic or unbounded labels
Bad:
```
request_duration{user_id="12345"}
```
Millions of unique user IDs.

Good:
```
request_duration{user_tier="premium"}
```

### 2. Container & microservice environments
```
container_cpu_usage{pod_id="pod‑abc123"}
```
Pods constantly restart, creating new IDs.

### 3. Debug labels left in production
```
api_call{trace_id="trace‑12345"}
```
Millions of trace IDs = disaster.

### 4. Raw URL values as labels
Bad:
```
http_request_duration{url="/users/12345/profile"}
```
Good:
```
http_request_duration{url="/users/{id}/profile"}
```

---

# Impact of High Cardinality

### 1. Storage explodes
- 1M series index = ~100 MB
- 25M series ≈ **2.5 GB index**
- One year of data = **50 TB**
- Storage bill ≈ **$1.15M/year**

### 2. Queries slow down massively
- Low-cardinality: scans 1k series → ms
- High-cardinality: scans 25M → seconds/minutes

### 3. Memory usage spikes
- 25M series → 2.5 GB index in memory
- Causes eviction, slows entire DB system

---

# Mitigation Strategies

### Strategy 1: Avoid high‑cardinality labels
Avoid:
- user_id  
- request_id  
- ip  
- timestamp  
- container_id  

Use:
- environment  
- service  
- region  
- normalized endpoints  

### Strategy 2: Bucket values
Bad:
```
latency{exact_ms=100.123}
```
Good:
```
latency_bucket{bucket="100–200ms"}
```

### Strategy 3: Cardinality limits
Prometheus rejects metrics over limit.

### Strategy 4: Cardinality audits
Track top offending metrics and fix them.

---

# Sampling Trade-offs

### Why sample?
Example: 100B spans/day  
→ 100 TB/day raw  
→ 36.5 PB/year (billions of dollars)

With 1% sampling:
- 1B spans/day  
- 1 TB/day  
- 365 TB/year  

### What you lose:
1. **Rare errors** vanish in sampling  
2. **Tail latencies** become invisible  
3. **Traces become incomplete**

---

## Head-based vs Tail-based Sampling

### Head-based (random at start)
Pros: Fast, simple, predictable  
Cons: Misses errors, misses slow traces

### Tail-based (decision after trace completes)
Pros:
- Keep all errors  
- Keep all slow traces  
- Intelligent sampling  

Cons:
- Must buffer all spans first  
- Expensive / memory heavy  

---

# Real-Time vs Batch Processing

Real-time needed for:
- Production incidents  
- Security attacks  
- Payments  
- User-facing issues  

Batch is fine for:
- Reports  
- Historical analysis  
- Post-mortems  
- Capacity planning  

Batch = 10× cheaper.

---

# Storage Optimization

### Compression
Raw: 20 TB/day  
Compressed 10×: 2 TB/day  
Queries 10× cheaper.

### Downsampling older data
- Last 7 days: full resolution  
- 8–30 days: 1‑min  
- 30–90 days: 1‑hour  
- 90+ days: archive/delete  

Saves ~90% storage.

---

# Query Performance at Scale

### Indexing
Without index: scan entire PB → minutes  
With index: jump to file → seconds

### Pre-aggregation
Fast but limited.

### On-demand aggregation
Flexible but slow.

### Hybrid = best.

---

# Alert Fatigue

Too many alerts → engineers ignore → miss real outages.

### Fix:
- Higher thresholds  
- Duration‑based alerts  
- Correlation alerts  
- Context-aware alerts  

---

# Context Correlation

### Challenge:
Metrics, logs, traces are separate.

### Fix:
Use consistent identifiers everywhere:
- **trace_id**  
- **service_name**  
- **request_id**  

This makes correlation possible.

---

# Tools Comparison

### Prometheus
- Cheap  
- Simple  
- Strict cardinality limits  
- Head-based sampling  

### DataDog
- Expensive  
- Automagic correlation  
- Tail-based smart sampling  
- High-cardinality support  

### ClickHouse
- Super fast  
- Supports very high cardinality  
- More control  

---

# What to Choose?

### Startup:
**Prometheus + Grafana**

### Growing company:
**DataDog or Grafana Cloud**

### Mega-scale:
**Custom ClickHouse/M3DB pipeline**

---



