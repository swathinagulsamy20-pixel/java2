# Project Assignment Set 19: International Port Container Terminal & Yard Engine

---

## 1. System Overview
The **International Port Container Terminal & Yard Engine** is a high-throughput, multi-threaded port logistics application designed for distributed cloud deployment across Oracle Cloud Infrastructure (OCI). The engine manages inbound vessel discharge, multi-tiered yard stacking, cold chain reefer telemetry, security audits, customs clearance, and gate-out dispatch.

---

## 2. Directory Structure

```
Java Project/
├── bin/                          # Compiled bytecode .class files
│   ├── DBConnection.class
│   ├── PortDAO.class
│   ├── CraneSchedulingWorker.class
│   ├── PassWriter.class
│   └── MainApp.class
├── data/
│   └── manifest.csv              # Inbound vessel manifest dataset
├── docs/
│   ├── ARCHITECTURE_VERIFICATION.md # OCI cloud architecture & firewall verification
│   └── EXECUTION_PROOFS.md       # Proof of execution logs, regex tests, multithreading
├── lib/
│   └── mysql-connector-j-26.10.0.jar # JDBC Driver
├── output/
│   ├── gate_passes.txt           # Exported gate-out clearance passes (String.format)
│   └── demurrage_billing.txt     # Exported demurrage fines & TCDI metrics
├── sql/
│   └── port_schema.sql           # Database schema (vessels, yard_slots, containers, gate_passes)
├── src/
│   ├── DBConnection.java         # JDBC factory with local and remote OCI VM support
│   ├── PortDAO.java              # DAO layer with CRUD, slot locking, and TCDI calculation
│   ├── CraneSchedulingWorker.java# Multithreaded parallel gantry crane workers
│   ├── PassWriter.java           # String processing, regex check, indexOf/substring parser, file I/O
│   └── MainApp.java              # Interactive and automated orchestration CLI
└── README.md                     # Project documentation
```

---

## 3. Submission Deliverables Mapping

| Required Deliverable | Project Path | Description |
| :--- | :--- | :--- |
| **1. Database Schema** | [`sql/port_schema.sql`](file:///C:/xampp/htdocs/25ITR151/Java%20Project/sql/port_schema.sql) | DDL schema defining `containers`, `vessels`, `yard_slots`, and `gate_passes` |
| **2. Java Source Files** | [`src/DBConnection.java`](file:///C:/xampp/htdocs/25ITR151/Java%20Project/src/DBConnection.java)<br>[`src/PortDAO.java`](file:///C:/xampp/htdocs/25ITR151/Java%20Project/src/PortDAO.java)<br>[`src/CraneSchedulingWorker.java`](file:///C:/xampp/htdocs/25ITR151/Java%20Project/src/CraneSchedulingWorker.java)<br>[`src/PassWriter.java`](file:///C:/xampp/htdocs/25ITR151/Java%20Project/src/PassWriter.java)<br>[`src/MainApp.java`](file:///C:/xampp/htdocs/25ITR151/Java%20Project/src/MainApp.java) | All 5 mandatory Java source files fully implemented |
| **3. Architecture Verification Document** | [`docs/ARCHITECTURE_VERIFICATION.md`](file:///C:/xampp/htdocs/25ITR151/Java%20Project/docs/ARCHITECTURE_VERIFICATION.md) | Proof of OCI VM1 <-> VM2 remote connection, ingress port 3306/5432, firewall rules |
| **4. Execution Proofs** | [`docs/EXECUTION_PROOFS.md`](file:///C:/xampp/htdocs/25ITR151/Java%20Project/docs/EXECUTION_PROOFS.md) | Verified logs for CSV import, regex checks, multithreading, and gate pass exports |

---

## 4. Technical Module Implementations

### A. String Processing
1. **ISO Container Code Regex Validation:**
   - Regex: `^[A-Z]{4}[0-9]{7}$`
   - Verified in `PassWriter.validateIsoCode()`
   - Drops non-compliant codes (e.g., `INVALID_ISO99`) with detailed security logs.
2. **Customs Text Parsing using `.indexOf()` & `.substring()`:**
   - Explicitly extracts key-value pairs (`DECL`, `SEAL_CODE`, `INSPECTION`, `CATEGORY`, `ORIGIN`) without using `split()`.
   - Implemented in `PassWriter.parseCustomsDoc()`.
3. **Formatted Gate Pass Generation via `String.format()`:**
   - Generates formatted terminal gate passes and itemized audit records.

### B. File Handling (I/O)
1. **Inbound Vessel Manifest:**
   - Reads `data/manifest.csv` containing multi-category container manifests.
2. **Export Files:**
   - `output/gate_passes.txt`: Official clearance passes for outbound haulier trucks.
   - `output/demurrage_billing.txt`: Comprehensive billing logs with overdue calculations.

### C. Multithreading
- `CraneSchedulingWorker.java` implements `Runnable`.
- Dispatches parallel crane worker threads across distinct yard blocks (`Block A`, `Block B`, `Block C`) simultaneously.
- Synchronized with `CountDownLatch` and thread-safe slot allocation.

### D. Mathematical Formulas
1. **Terminal Container Dwell Index (TCDI):**
   $$\text{TCDI} = \frac{\sum \text{IndividualContainerDwellDays}}{\text{TotalProcessedContainers}}$$
2. **Total Demurrage Fine:**
   $$\text{TotalDemurrageFine} = (\text{ActualDwellDays} - \text{FreeDaysCap}) \times \text{DailyDemurrageRate} \quad (\text{if Actual} > \text{Free Cap})$$

---

## 5. How to Compile and Run

### 1. Initialize the Database
Ensure MySQL is running (port 3306) and execute:
```powershell
cmd /c "mysql -u root -pMysql@12345 < sql\port_schema.sql"
```

### 2. Compile Java Source Files
```powershell
javac -cp "lib/mysql-connector-j-26.10.0.jar" -d bin src/*.java
```

### 3. Run the Automated Pipeline
```powershell
java -cp "bin;lib/mysql-connector-j-26.10.0.jar" MainApp --auto
```

### 4. Run Interactive Console Menu
```powershell
java -cp "bin;lib/mysql-connector-j-26.10.0.jar" MainApp
```
