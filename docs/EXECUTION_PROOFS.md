# Execution Proofs Report
## Project Assignment Set 19: International Port Container Terminal & Yard Engine

---

### Deliverable Summary
This document provides formal execution proofs demonstrating compliance with all technical module requirements and core functional evaluation specifications.

---

### 1. Inbound Vessel Manifest CSV Import Proof

**Source File:** `data/manifest.csv`  
The application reads inbound manifests containing container ISO codes, voyages, categories, gross weights, customs documentation text, seal statuses, and category metrics.

```csv
ContainerID,VoyageID,CargoCategory,WeightTons,YardBlock,CustomsDocText,CustomsSealStatus,ReeferPowerKW,OffPowerMinutes,ActualDwellDays,DailyDemurrageRate,Factor1,Factor2
MSKU1029384,VOY-EVER-2026,REEFER,26.4,Block A,DECL:[DEC-2026-901];SEAL_CODE:[SL-OK-9012];INSPECTION:[CLEARED_GREEN];CATEGORY:[REEFER];ORIGIN:[SGSIN],INTACT,8.5,12,3,200.0,95.0,92.0
MSCU4829105,VOY-MAER-8841,STANDARD_DRY,22.1,Block B,DECL:[DEC-2026-902];SEAL_CODE:[SL-OK-9033];INSPECTION:[STANDARD_VERIFIED];CATEGORY:[STANDARD_DRY];ORIGIN:[NLRTM],INTACT,0.0,0,5,150.0,88.0,85.0
CMAU7718290,VOY-CMA-4420,HAZMAT,28.6,Block C,DECL:[DEC-2026-903];SEAL_CODE:[SL-OK-9044];INSPECTION:[CLASS_3_FLAMMABLE_PASS];CATEGORY:[HAZMAT];ORIGIN:[CNSHA],INTACT,0.0,0,2,300.0,92.0,90.0
HLCU6619201,VOY-EVER-2026,STANDARD_DRY,24.0,Block B,DECL:[DEC-2026-904];SEAL_CODE:[SL-OK-9055];INSPECTION:[ROUTINE_CLEARANCE];CATEGORY:[STANDARD_DRY];ORIGIN:[SGSIN],INTACT,0.0,0,10,150.0,78.0,75.0
OOLU9918274,VOY-MAER-8841,REEFER,25.8,Block A,DECL:[DEC-2026-905];SEAL_CODE:[SL-TMP-9066];INSPECTION:[SEAL_CORRUPT_FLAG];CATEGORY:[REEFER];ORIGIN:[NLRTM],TAMPERED,7.2,45,6,220.0,60.0,55.0
ZIMU5544332,VOY-CMA-4420,HAZMAT,19.5,Block C,DECL:[DEC-2026-906];SEAL_CODE:[SL-OK-9077];INSPECTION:[CHEMICAL_HAZARD_REVIEW];CATEGORY:[HAZMAT];ORIGIN:[CNSHA],INTACT,0.0,0,4,350.0,85.0,80.0
ONEU3322110,VOY-EVER-2026,STANDARD_DRY,21.0,Block B,DECL:[DEC-2026-907];SEAL_CODE:[SL-OK-9088];INSPECTION:[CUSTOMS_PASSED_PRIORITY];CATEGORY:[STANDARD_DRY];ORIGIN:[SGSIN],INTACT,0.0,0,4,150.0,94.0,92.0
APLU8877665,VOY-MAER-8841,REEFER,27.0,Block A,DECL:[DEC-2026-908];SEAL_CODE:[SL-OK-9099];INSPECTION:[COLD_CHAIN_APPROVED];CATEGORY:[REEFER];ORIGIN:[NLRTM],INTACT,9.1,10,2,200.0,98.0,95.0
INVALID_ISO99,VOY-EVER-2026,STANDARD_DRY,20.0,Block B,DECL:[DEC-2026-909];SEAL_CODE:[SL-BAD-0000];INSPECTION:[INVALID_ID];CATEGORY:[STANDARD_DRY];ORIGIN:[SGSIN],INTACT,0.0,0,3,150.0,50.0,50.0
```

---

### 2. Container ISO Code Regex Verification Proof

**Regex Specification:** `([A-Z]{4}[0-9]{7})`  
**Implementation:** `PassWriter.validateIsoCode(containerCode)`

#### Execution Console Output:
```text
Loading Vessel Manifest from: manifest.csv
ISO Validation Regex Applied: ^[A-Z]{4}[0-9]{7}$
-------------------------------------------------------------------------------
 [PHASE 1 ACCEPTED] MSKU1029384  | Voyage: VOY-EVER-2026  | REEFER       |  26.4 T | Free Cap: 3 Days | Block A
 [PHASE 1 ACCEPTED] MSCU4829105  | Voyage: VOY-MAER-8841  | STANDARD_DRY |  22.1 T | Free Cap: 7 Days | Block B
 [PHASE 1 ACCEPTED] CMAU7718290  | Voyage: VOY-CMA-4420   | HAZMAT       |  28.6 T | Free Cap: 2 Days | Block C
 [PHASE 1 ACCEPTED] HLCU6619201  | Voyage: VOY-EVER-2026  | STANDARD_DRY |  24.0 T | Free Cap: 7 Days | Block B
 [PHASE 1 ACCEPTED] OOLU9918274  | Voyage: VOY-MAER-8841  | REEFER       |  25.8 T | Free Cap: 3 Days | Block A
 [PHASE 1 ACCEPTED] ZIMU5544332  | Voyage: VOY-CMA-4420   | HAZMAT       |  19.5 T | Free Cap: 2 Days | Block C
 [PHASE 1 ACCEPTED] ONEU3322110  | Voyage: VOY-EVER-2026  | STANDARD_DRY |  21.0 T | Free Cap: 7 Days | Block B
 [PHASE 1 ACCEPTED] APLU8877665  | Voyage: VOY-MAER-8841  | REEFER       |  27.0 T | Free Cap: 3 Days | Block A
[ISO REGEX REJECTION] Line 10: 'INVALID_ISO99' violates ISO 6346 code standard ([A-Z]{4}[0-9]{7}). Dropped from manifest.
-------------------------------------------------------------------------------
Phase 1 Import Completed: 8 Accepted, 1 Rejected.
```

---

### 3. Customs String Processing Proof (`.indexOf()` and `.substring()`)

**Implementation:** `PassWriter.parseCustomsDoc(docText)`  
Parses encoded customs strings:

```text
Container ID: MSKU1029384
 Raw Customs Doc: DECL:[DEC-2026-901];SEAL_CODE:[SL-OK-9012];INSPECTION:[CLEARED_GREEN];CATEGORY:[REEFER];ORIGIN:[SGSIN]
 Parsed Key-Value Pairs:
   - DECL            => DEC-2026-901
   - SEAL_CODE       => SL-OK-9012
   - INSPECTION      => CLEARED_GREEN
   - CATEGORY        => REEFER
   - ORIGIN          => SGSIN
```

---

### 4. Parallel Multithreaded Crane Scheduling Execution Logs

**Implementation:** `CraneSchedulingWorker.java` running on parallel worker threads simultaneously managing Block A, Block B, and Block C.

```text
>>> [STEP 3/5] Phase 2: Parallel Multithreaded Gantry Crane Movement Scheduling...
-------------------------------------------------------------------------------
   PHASE 2: PARALLEL MULTITHREADED GANTRY CRANE MOVEMENT SCHEDULING           
-------------------------------------------------------------------------------
[CraneThread-BlockC] Gantry Crane CRANE-GC-01 ACTIVATED for Block C (Containers to process: 2)
[CraneThread-BlockB] Gantry Crane CRANE-GC-02 ACTIVATED for Block B (Containers to process: 3)
[CraneThread-BlockA] Gantry Crane CRANE-GC-03 ACTIVATED for Block A (Containers to process: 3)
[CraneThread-BlockC | CRANE-GC-01] Stacking CMAU7718290 -> Slot: BLK-C-H01-T1 | Category: HAZMAT       | Dwell: 2 days | Score: 92.2 | Band: Port Priority Clear (Fast-Track Gate Out)
[CraneThread-BlockB | CRANE-GC-02] Stacking MSCU4829105 -> Slot: BLK-B-D01-T1 | Category: STANDARD_DRY | Dwell: 5 days | Score: 92.2 | Band: Port Priority Clear (Fast-Track Gate Out)
[CraneThread-BlockA | CRANE-GC-03] Stacking MSKU1029384 -> Slot: BLK-A-R01-T1 | Category: REEFER       | Dwell: 3 days | Score: 95.1 | Band: Port Priority Clear (Fast-Track Gate Out)
[CraneThread-BlockC | CRANE-GC-01] Stacking ZIMU5544332 -> Slot: BLK-C-H02-T1 | Category: HAZMAT       | Dwell: 4 days | Score: 82.0 | Band: Standard Flow (Normal Yard Clearance)
[CraneThread-BlockC] Gantry Crane CRANE-GC-01 COMPLETED block operation for Block C.
[CraneThread-BlockB | CRANE-GC-02] Stacking HLCU6619201 -> Slot: BLK-B-D02-T1 | Category: STANDARD_DRY | Dwell: 10 days | Score: 68.2 | Band: Delayed Clearance (Demurrage Warning Issued)
[CraneThread-BlockA | CRANE-GC-03] Stacking OOLU9918274 -> Slot: BLK-A-R02-T1 | Category: REEFER       | Dwell: 6 days | Score: 45.5 | Band: Seized / Audited (Container Blocked)
[CraneThread-BlockB | CRANE-GC-02] Stacking ONEU3322110 -> Slot: BLK-B-D01-T2 | Category: STANDARD_DRY | Dwell: 4 days | Score: 96.0 | Band: Port Priority Clear (Fast-Track Gate Out)
[CraneThread-BlockB] Gantry Crane CRANE-GC-02 COMPLETED block operation for Block B.
[CraneThread-BlockA | CRANE-GC-03] Stacking APLU8877665 -> Slot: BLK-A-R01-T2 | Category: REEFER       | Dwell: 2 days | Score: 97.5 | Band: Port Priority Clear (Fast-Track Gate Out)
[CraneThread-BlockA] Gantry Crane CRANE-GC-03 COMPLETED block operation for Block A.

[MULTITHREADING SYNC] All parallel gantry crane movement cycles completed successfully.
```

---

### 5. Phase 3 Gate Dispatch & Tampered Security Interception

```text
>>> [STEP 4/5] Phase 3: Outbound Truck Gate Clearance & Pass Generation...
-------------------------------------------------------------------------------
   PHASE 3: OUTBOUND TRUCK REGISTRATION & GATE CLEARANCE PASS ISSUANCE         
-------------------------------------------------------------------------------
 [GATE PASS ISSUED] GP-2026-0001 -> APLU8877665 | Truck: TN-33-AX-8910 | Dwell: 2/3 d | Demurrage: $0.00 | Band: Port Priority Clear (Fast-Track Gate Out)
 [GATE PASS ISSUED] GP-2026-0002 -> CMAU7718290 | Truck: MH-04-CK-4021 | Dwell: 2/2 d | Demurrage: $0.00 | Band: Port Priority Clear (Fast-Track Gate Out)
 [GATE PASS ISSUED] GP-2026-0003 -> HLCU6619201 | Truck: KA-01-MJ-9912 | Dwell: 10/7 d | Demurrage: $450.00 | Band: Delayed Clearance (Demurrage Warning Issued)
 [GATE PASS ISSUED] GP-2026-0004 -> MSCU4829105 | Truck: DL-10-BT-1122 | Dwell: 5/7 d | Demurrage: $0.00 | Band: Port Priority Clear (Fast-Track Gate Out)
 [GATE PASS ISSUED] GP-2026-0005 -> MSKU1029384 | Truck: WB-02-QA-7744 | Dwell: 3/3 d | Demurrage: $0.00 | Band: Port Priority Clear (Fast-Track Gate Out)
 [GATE PASS ISSUED] GP-2026-0006 -> ONEU3322110 | Truck: KL-07-ZZ-3311 | Dwell: 4/7 d | Demurrage: $0.00 | Band: Port Priority Clear (Fast-Track Gate Out)
[SECURITY INTERVENTION] Container OOLU9918274 customs seal is TAMPERED / Score 45.5! GATE PASS REFUSED -> CONTAINER SEIZED FOR AUDIT.
 [GATE PASS ISSUED] GP-2026-0007 -> ZIMU5544332 | Truck: AP-16-EE-5544 | Dwell: 4/2 d | Demurrage: $700.00 | Band: Standard Flow (Normal Yard Clearance)
-------------------------------------------------------------------------------
Exported 7 Gate Passes to: output/gate_passes.txt
Exported Demurrage Billing Log to: output/demurrage_billing.txt
```

---

### 6. Mathematical Verification: TCDI & Demurrage Billing

#### A. Terminal Container Dwell Index (TCDI)
$$\text{TCDI} = \frac{\sum \text{IndividualContainerDwellDays}}{\text{TotalProcessedContainers}}$$
$$\text{TCDI} = \frac{2 + 2 + 10 + 5 + 3 + 4 + 6 + 4}{8} = \frac{36}{8} = \mathbf{4.50 \text{ Days / Container}}$$

#### B. Total Demurrage Fine
$$\text{TotalDemurrageFine} = (\text{ActualDwellDays} - \text{FreeDaysCap}) \times \text{DailyDemurrageRate}$$
- **HLCU6619201**: $(10 - 7) \times \$150.00 = \mathbf{\$450.00}$
- **OOLU9918274**: $(6 - 3) \times \$220.00 = \mathbf{\$660.00}$
- **ZIMU5544332**: $(4 - 2) \times \$350.00 = \mathbf{\$700.00}$
- **Total Demurrage Revenue**: $\$450.00 + \$660.00 + \$700.00 = \mathbf{\$1,810.00}$

---

### 7. Exported Gate Pass Sample (`output/gate_passes.txt`)

```text
+-------------------------------------------------------------------------+
|                   INTERNATIONAL PORT CONTAINER TERMINAL                 |
|                      OFFICIAL GATE DISPATCH PASS                        |
+-------------------------------------------------------------------------+
| Pass Identifier       : GP-2026-0001                                    |
| Gate Out Timestamp    : 2026-11-09 14:32:21.0                           |
| Issuing Authority     : PORT_STATION_DESK_01                            |
+-------------------------------------------------------------------------+
| Container ISO Code    : APLU8877665                                     |
| Cargo Category        : REEFER                                          |
| Gross Weight          : 27.00                                           Tons |
| Yard Storage Slot     : BLK-A-R01-T2                                    |
+-------------------------------------------------------------------------+
| Haulier Truck Reg No  : TN-33-AX-8910                                   |
| Driver License No     : DL-IND-901234                                   |
| Customs Release Stamp : CUSTOMS-REL-AUTH-202602                         |
+-------------------------------------------------------------------------+
| Actual Dwell Duration : 2  Days (Free Cap: 3 Days)                       |
| Daily Demurrage Rate  : $200.00                                         |
| Demurrage Penalty Due : $0.00                                           |
| Performance Score     : 97.50 / 100.0                                    |
| Operational Band      : Port Priority Clear (Fast-Track Gate Out)       |
| Gate Clearance Status : FAST-TRACK EXIT APPROVED                        |
+-------------------------------------------------------------------------+
| Remarks               : WITHIN FREE DWELL ALLOWANCE - NORMAL EXIT       |
+-------------------------------------------------------------------------+
```
