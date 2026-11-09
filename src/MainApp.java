import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

/**
 * MainApp: Execution Driver for Project Assignment Set 19:
 * International Port Container Terminal & Yard Engine.
 * 
 * Implements:
 * 1. Cloud Architecture Verification (OCI VM1 Application Host <-> OCI VM2 DB Host)
 * 2. Phased Incremental Data Entry (Phase 1 Unloading, Phase 2 Yard Storage, Phase 3 Gate Dispatch)
 * 3. Technical Module Requirements:
 *    - String Processing: ISO Container Code regex, indexOf()/substring() parsing, String.format()
 *    - File Handling (I/O): manifest.csv reading, gate_passes.txt and demurrage_billing.txt export
 *    - Multithreading: Parallel gantry crane scheduling workers
 * 4. Demurrage Penalty & Terminal Container Dwell Index (TCDI) Mathematical Models
 */
public class MainApp {

    private final PortDAO portDAO;
    private final List<PortDAO.ContainerRecord> validContainers;
    private final List<String> rejectedContainers;

    public MainApp() {
        this.portDAO = new PortDAO();
        this.validContainers = new ArrayList<>();
        this.rejectedContainers = new ArrayList<>();
    }

    public static void main(String[] args) {
        MainApp app = new MainApp();

        boolean autoMode = false;
        for (String arg : args) {
            if ("--auto".equalsIgnoreCase(arg) || "-a".equalsIgnoreCase(arg)) {
                autoMode = true;
                break;
            }
        }

        if (autoMode) {
            System.out.println("===============================================================================");
            System.out.println("   INTERNATIONAL PORT CONTAINER TERMINAL & YARD ENGINE - AUTOMATED PIPELINE    ");
            System.out.println("===============================================================================");
            app.runFullPipeline();
            return;
        }

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\n===============================================================================");
            System.out.println("   PROJECT SET 19: INTERNATIONAL PORT CONTAINER TERMINAL & YARD ENGINE        ");
            System.out.println("===============================================================================");
            System.out.println(" 1. Run Complete End-to-End Pipeline (Phases 1, 2, 3 + File I/O + Multithreading)");
            System.out.println(" 2. Phase 1: Inbound Vessel Manifest Import (manifest.csv) & ISO Regex Check");
            System.out.println(" 3. String Processing Demo: Customs Doc Parsing via indexOf() and substring()");
            System.out.println(" 4. Phase 2: Multithreaded Parallel Gantry Crane Movement Scheduling");
            System.out.println(" 5. Phase 3: Gate Clearance Dispatch & Text File Generation (gate_passes.txt)");
            System.out.println(" 6. Mathematical Formulas: Demurrage Audit & Dwell Index (TCDI)");
            System.out.println(" 7. Test Remote Database Connection (OCI Cloud Host Verification)");
            System.out.println(" 0. Exit");
            System.out.print("\nEnter your choice [0-7]: ");

            String input = scanner.hasNextLine() ? scanner.nextLine().trim() : "0";
            switch (input) {
                case "1":
                    app.runFullPipeline();
                    break;
                case "2":
                    app.executePhase1Import("data/manifest.csv");
                    break;
                case "3":
                    app.demonstrateCustomsStringParsing();
                    break;
                case "4":
                    app.executePhase2MultithreadedCranes();
                    break;
                case "5":
                    app.executePhase3GateDispatch();
                    break;
                case "6":
                    app.displayDemurrageAndTCDI();
                    break;
                case "7":
                    app.testCloudConnection();
                    break;
                case "0":
                    System.out.println("\nExiting Port Operations Engine. Goodbye!");
                    return;
                default:
                    System.out.println("Invalid option. Please choose between 0 and 7.");
            }
        }
    }

    /**
     * Executes the complete end-to-end terminal pipeline.
     */
    public void runFullPipeline() {
        System.out.println("\n>>> [STEP 1/5] Testing Database Connectivity...");
        if (!DBConnection.testConnection()) {
            System.err.println("[FAIL] Cannot connect to database at: " + DBConnection.getJdbcUrl());
            System.err.println("Please verify MySQL is running and port_terminal_db is initialized.");
            return;
        }
        System.out.println("[PASS] Database connected successfully to: " + DBConnection.getJdbcUrl());

        // Reset operational state for fresh run
        portDAO.resetOperationalState();

        System.out.println("\n>>> [STEP 2/5] Phase 1: Inbound Vessel Manifest Import & ISO Regex Validation...");
        executePhase1Import("data/manifest.csv");

        System.out.println("\n>>> [STEP 3/5] Phase 2: Parallel Multithreaded Gantry Crane Movement Scheduling...");
        executePhase2MultithreadedCranes();

        System.out.println("\n>>> [STEP 4/5] Phase 3: Outbound Truck Gate Clearance & Pass Generation...");
        executePhase3GateDispatch();

        System.out.println("\n>>> [STEP 5/5] Mathematical Audit: Terminal Container Dwell Index (TCDI) & Demurrage...");
        displayDemurrageAndTCDI();

        System.out.println("\n[SUCCESS] Entire Port Operations Lifecycle Executed Successfully!");
        System.out.println("Generated Deliverables:");
        System.out.println(" - output/gate_passes.txt");
        System.out.println(" - output/demurrage_billing.txt");
    }

    /**
     * Phase 1: Reads manifest.csv, applies regex check ([A-Z]{4}[0-9]{7}),
     * parses customs text, assigns Free Dwell Cap per category, and persists to DB.
     */
    public void executePhase1Import(String csvPath) {
        File file = new File(csvPath);
        if (!file.exists()) {
            System.err.println("[ERROR] Manifest file not found: " + file.getAbsolutePath());
            return;
        }

        validContainers.clear();
        rejectedContainers.clear();

        System.out.println("-------------------------------------------------------------------------------");
        System.out.printf("Loading Vessel Manifest from: %s%n", file.getName());
        System.out.printf("ISO Validation Regex Applied: %s%n", PassWriter.ISO_REGEX);
        System.out.println("-------------------------------------------------------------------------------");

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String headerLine = reader.readLine(); // skip header
            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) continue;

                String[] tokens = line.split(",");
                if (tokens.length < 13) {
                    System.err.printf("[Line %d] Incomplete columns, skipping line.%n", lineNumber);
                    continue;
                }

                String containerId = tokens[0].trim();
                String voyageId = tokens[1].trim();
                String category = tokens[2].trim().toUpperCase();
                double weightTons = Double.parseDouble(tokens[3].trim());
                String yardBlock = tokens[4].trim();
                String customsDocText = tokens[5].trim();
                String sealStatus = tokens[6].trim().toUpperCase();
                double reeferPowerKw = Double.parseDouble(tokens[7].trim());
                int offPowerMinutes = Integer.parseInt(tokens[8].trim());
                int dwellDays = Integer.parseInt(tokens[9].trim());
                double dailyDemurrageRate = Double.parseDouble(tokens[10].trim());
                double factor1 = Double.parseDouble(tokens[11].trim());
                double factor2 = Double.parseDouble(tokens[12].trim());

                // Regex Validation Check
                if (!PassWriter.validateIsoCode(containerId)) {
                    System.out.printf("[ISO REGEX REJECTION] Line %d: '%s' violates ISO 6346 code standard ([A-Z]{4}[0-9]{7}). Dropped from manifest.%n",
                            lineNumber, containerId);
                    rejectedContainers.add(containerId);
                    continue;
                }

                // Determine Free Dwell Cap according to Category Specification:
                // REEFER: 3 Days | DRY FREIGHT: 7 Days | HAZMAT: 2 Days
                int freeDwellCap;
                switch (category) {
                    case "REEFER":
                        freeDwellCap = 3;
                        break;
                    case "HAZMAT":
                        freeDwellCap = 2;
                        break;
                    case "STANDARD_DRY":
                    default:
                        freeDwellCap = 7;
                        break;
                }

                PortDAO.ContainerRecord c = new PortDAO.ContainerRecord();
                c.containerId = containerId;
                c.voyageId = voyageId;
                c.cargoCategory = category;
                c.weightTons = weightTons;
                c.yardBlock = yardBlock;
                c.customsDocText = customsDocText;
                c.customsSealStatus = sealStatus;
                c.reeferPowerKw = reeferPowerKw;
                c.offPowerMinutes = offPowerMinutes;
                c.dwellDays = dwellDays;
                c.freeDwellCap = freeDwellCap;
                c.dailyDemurrageRate = dailyDemurrageRate;
                c.factor1 = factor1;
                c.factor2 = factor2;

                boolean inserted = portDAO.registerContainerPhase1(c);
                if (inserted) {
                    validContainers.add(c);
                    System.out.printf(" [PHASE 1 ACCEPTED] %-12s | Voyage: %-14s | %-12s | %5.1f T | Free Cap: %d Days | %s%n",
                            containerId, voyageId, category, weightTons, freeDwellCap, yardBlock);
                }
            }

            System.out.println("-------------------------------------------------------------------------------");
            System.out.printf("Phase 1 Import Completed: %d Accepted, %d Rejected.%n", validContainers.size(), rejectedContainers.size());

        } catch (IOException e) {
            System.err.println("[ERROR] Failed reading manifest.csv: " + e.getMessage());
        }
    }

    /**
     * Demonstrates parsing customs documentation text using String indexOf() and substring().
     */
    public void demonstrateCustomsStringParsing() {
        System.out.println("-------------------------------------------------------------------------------");
        System.out.println("   CUSTOMS DOCUMENTATION STRING PARSING USING indexOf() AND substring()       ");
        System.out.println("-------------------------------------------------------------------------------");

        if (validContainers.isEmpty()) {
            System.out.println("No containers loaded. Running Phase 1 manifest import first...");
            executePhase1Import("data/manifest.csv");
        }

        for (PortDAO.ContainerRecord c : validContainers) {
            System.out.printf("Container ID: %s%n", c.containerId);
            System.out.printf(" Raw Customs Doc: %s%n", c.customsDocText);

            Map<String, String> parsed = PassWriter.parseCustomsDoc(c.customsDocText);
            System.out.println(" Parsed Key-Value Pairs:");
            for (Map.Entry<String, String> entry : parsed.entrySet()) {
                System.out.printf("   - %-15s => %s%n", entry.getKey(), entry.getValue());
            }
            System.out.println();
        }
    }

    /**
     * Phase 2: Spawns parallel worker threads per yard block to simulate gantry cranes.
     */
    public void executePhase2MultithreadedCranes() {
        System.out.println("-------------------------------------------------------------------------------");
        System.out.println("   PHASE 2: PARALLEL MULTITHREADED GANTRY CRANE MOVEMENT SCHEDULING           ");
        System.out.println("-------------------------------------------------------------------------------");

        if (validContainers.isEmpty()) {
            System.out.println("No containers in memory. Running Phase 1 manifest import first...");
            executePhase1Import("data/manifest.csv");
        }

        // Group containers by Yard Block
        Map<String, List<PortDAO.ContainerRecord>> blockBuckets = new HashMap<>();
        for (PortDAO.ContainerRecord c : validContainers) {
            blockBuckets.computeIfAbsent(c.yardBlock, k -> new ArrayList<>()).add(c);
        }

        int workerCount = blockBuckets.size();
        CountDownLatch latch = new CountDownLatch(workerCount);
        List<Thread> craneThreads = new ArrayList<>();

        int craneNum = 1;
        for (Map.Entry<String, List<PortDAO.ContainerRecord>> entry : blockBuckets.entrySet()) {
            String blockName = entry.getKey();
            List<PortDAO.ContainerRecord> blockContainers = entry.getValue();
            String craneId = "CRANE-GC-0" + craneNum;

            CraneSchedulingWorker worker = new CraneSchedulingWorker(craneId, blockName, blockContainers, portDAO, latch);
            Thread t = new Thread(worker, "CraneThread-" + blockName.replace(" ", ""));
            craneThreads.add(t);
            t.start();
            craneNum++;
        }

        // Wait for all crane workers to complete their physical placement schedules
        try {
            latch.await();
            System.out.println("\n[MULTITHREADING SYNC] All parallel gantry crane movement cycles completed successfully.");
        } catch (InterruptedException e) {
            System.err.println("[ERROR] Multithreading execution interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Phase 3: Outbound Truck Registration, Customs Release Clearance Stamp,
     * Demurrage Calculation, Gate-Out Time, and .txt File Exports.
     */
    public void executePhase3GateDispatch() {
        System.out.println("-------------------------------------------------------------------------------");
        System.out.println("   PHASE 3: OUTBOUND TRUCK REGISTRATION & GATE CLEARANCE PASS ISSUANCE         ");
        System.out.println("-------------------------------------------------------------------------------");

        List<PortDAO.ContainerRecord> currentContainers = portDAO.getAllContainers();
        if (currentContainers.isEmpty()) {
            System.out.println("No containers in database. Please run Phase 1 and 2 first.");
            return;
        }

        Map<String, PortDAO.ContainerRecord> containerMap = new HashMap<>();
        for (PortDAO.ContainerRecord c : currentContainers) {
            containerMap.put(c.containerId, c);
        }

        String[] sampleTrucks = {
                "TN-33-AX-8910", "MH-04-CK-4021", "KA-01-MJ-9912",
                "DL-10-BT-1122", "WB-02-QA-7744", "KL-07-ZZ-3311",
                "AP-16-EE-5544", "GJ-06-PP-9900"
        };
        String[] sampleDrivers = {
                "DL-IND-901234", "DL-IND-445566", "DL-IND-778899",
                "DL-IND-223344", "DL-IND-990011", "DL-IND-334455",
                "DL-IND-667788", "DL-IND-112233"
        };

        int passCounter = 1;
        int truckIndex = 0;

        for (PortDAO.ContainerRecord c : currentContainers) {
            // Check Security / Tamper Audit Filter
            if ("TAMPERED".equalsIgnoreCase(c.customsSealStatus) || "BLOCKED_AUDIT".equalsIgnoreCase(c.status)) {
                System.out.printf("[SECURITY INTERVENTION] Container %s customs seal is TAMPERED / Score %.1f! GATE PASS REFUSED -> CONTAINER SEIZED FOR AUDIT.%n",
                        c.containerId, c.performanceScore);
                continue;
            }

            String passId = String.format("GP-2026-%04d", passCounter++);
            String truckReg = sampleTrucks[truckIndex % sampleTrucks.length];
            String driverLic = sampleDrivers[truckIndex % sampleDrivers.length];
            truckIndex++;

            String clearanceStamp = "CUSTOMS-REL-AUTH-" + (202600 + passCounter);
            Timestamp gateOutTime = new Timestamp(System.currentTimeMillis() + (passCounter * 60000L));

            // Demurrage calculation using mathematical formula
            double demurrageFine = PassWriter.calculateDemurrageFine(c.dwellDays, c.freeDwellCap, c.dailyDemurrageRate);

            String remarks;
            if (demurrageFine > 0) {
                remarks = String.format("DEMURRAGE PENALTY COLLECTED ($%.2f for %d days overdue)",
                        demurrageFine, (c.dwellDays - c.freeDwellCap));
            } else {
                remarks = "WITHIN FREE DWELL ALLOWANCE - NORMAL EXIT";
            }

            PortDAO.GatePassRecord pass = new PortDAO.GatePassRecord(
                    passId,
                    c.containerId,
                    truckReg,
                    driverLic,
                    clearanceStamp,
                    c.dwellDays,
                    c.freeDwellCap,
                    c.dailyDemurrageRate,
                    demurrageFine,
                    c.performanceScore,
                    c.performanceBand,
                    gateOutTime,
                    "PORT_STATION_DESK_01",
                    remarks
            );

            boolean dispatched = portDAO.recordGateDispatchPhase3(pass);
            if (dispatched) {
                System.out.printf(" [GATE PASS ISSUED] %s -> %s | Truck: %s | Dwell: %d/%d d | Demurrage: $%.2f | Band: %s%n",
                        passId, c.containerId, truckReg, c.dwellDays, c.freeDwellCap, demurrageFine, c.performanceBand);
            }
        }

        // Export generated passes and demurrage billing logs
        List<PortDAO.GatePassRecord> allPasses = portDAO.getAllGatePasses();
        double tcdi = portDAO.calculateTerminalContainerDwellIndex();

        String passesFilePath = "output/gate_passes.txt";
        String billingFilePath = "output/demurrage_billing.txt";

        PassWriter.exportGatePassesToFile(allPasses, containerMap, passesFilePath);
        PassWriter.exportDemurrageBillingToFile(currentContainers, tcdi, billingFilePath);

        System.out.println("-------------------------------------------------------------------------------");
        System.out.printf("Exported %d Gate Passes to: %s%n", allPasses.size(), passesFilePath);
        System.out.printf("Exported Demurrage Billing Log to: %s%n", billingFilePath);
    }

    /**
     * Mathematical Formula Display: TCDI and Total Demurrage Fine.
     */
    public void displayDemurrageAndTCDI() {
        System.out.println("-------------------------------------------------------------------------------");
        System.out.println("   MATHEMATICAL FORMULAS & TERMINAL EFFICIENCY METRICS                         ");
        System.out.println("-------------------------------------------------------------------------------");

        List<PortDAO.ContainerRecord> containers = portDAO.getAllContainers();
        if (containers.isEmpty()) {
            System.out.println("No containers found in database. Run Phase 1 first.");
            return;
        }

        int totalContainers = containers.size();
        int sumDwellDays = 0;
        double totalDemurrage = 0.0;
        int penalizedCount = 0;

        System.out.printf("%-14s | %-13s | %-6s | %-6s | %-12s | %-12s | %s%n",
                "Container ID", "Category", "Dwell", "Cap", "Daily Rate", "Total Fine", "Performance Band");
        System.out.println("--------------------------------------------------------------------------------------------------");

        for (PortDAO.ContainerRecord c : containers) {
            sumDwellDays += c.dwellDays;
            double fine = PassWriter.calculateDemurrageFine(c.dwellDays, c.freeDwellCap, c.dailyDemurrageRate);
            if (fine > 0) {
                totalDemurrage += fine;
                penalizedCount++;
            }

            System.out.printf("%-14s | %-13s | %-6d | %-6d | $%-11.2f | $%-11.2f | %s%n",
                    c.containerId, c.cargoCategory, c.dwellDays, c.freeDwellCap, c.dailyDemurrageRate, fine, c.performanceBand);
        }

        // Terminal Container Dwell Index: TCDI = (Sum IndividualContainerDwellDays) / TotalProcessedContainers
        double tcdi = (double) sumDwellDays / totalContainers;

        System.out.println("--------------------------------------------------------------------------------------------------");
        System.out.printf("1. Terminal Container Dwell Index (TCDI):%n");
        System.out.printf("   Formula: TCDI = (Sum IndividualContainerDwellDays) / TotalProcessedContainers%n");
        System.out.printf("   Calculation: %d Days / %d Containers = %.2f Days / Container%n%n",
                sumDwellDays, totalContainers, tcdi);

        System.out.printf("2. Demurrage Fine Formula:%n");
        System.out.printf("   Formula: TotalDemurrageFine = (ActualDwellDays - FreeDaysCap) * DailyDemurrageRate%n");
        System.out.printf("   Containers Penaltized: %d / %d%n", penalizedCount, totalContainers);
        System.out.printf("   Total Terminal Demurrage Incurred: $%.2f%n", totalDemurrage);
        System.out.println("-------------------------------------------------------------------------------");
    }

    /**
     * Demonstrates OCI VM Remote Database Connectivity Test.
     */
    public void testCloudConnection() {
        System.out.println("-------------------------------------------------------------------------------");
        System.out.println("   OCI ARCHITECTURE REMOTE DATABASE CONNECTION VERIFICATION                    ");
        System.out.println("   Student 1 (App Host VM 1) <---> Student 2 (Database Host VM 2: Port 3306)   ");
        System.out.println("-------------------------------------------------------------------------------");
        System.out.printf("Current Configured Target Host : %s%n", DBConnection.getHost());
        System.out.printf("Current Configured Target Port : %s%n", DBConnection.getPort());
        System.out.printf("Current Target Database Name   : %s%n", DBConnection.getDbName());
        System.out.printf("Current Target User            : %s%n", DBConnection.getUser());
        System.out.printf("Testing JDBC URL               : %s%n", DBConnection.getJdbcUrl());

        long startTime = System.currentTimeMillis();
        boolean success = DBConnection.testConnection();
        long duration = System.currentTimeMillis() - startTime;

        if (success) {
            System.out.printf("[VERIFIED] Remote JDBC handshake succeeded in %d ms! Network routing open.%n", duration);
        } else {
            System.out.printf("[FAILED] Remote connection failed after %d ms. Check OCI Ingress / iptables rules.%n", duration);
        }
    }
}
