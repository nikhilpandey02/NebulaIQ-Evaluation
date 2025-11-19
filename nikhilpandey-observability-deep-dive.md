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
