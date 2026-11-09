import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * CraneSchedulingWorker implements parallel multithreaded scheduling for
 * gantry cranes servicing designated yard blocks simultaneously.
 */
public class CraneSchedulingWorker implements Runnable {

    private final String craneId;
    private final String yardBlock;
    private final List<PortDAO.ContainerRecord> containersToSchedule;
    private final PortDAO portDAO;
    private final CountDownLatch completionLatch;

    public CraneSchedulingWorker(String craneId, String yardBlock,
                                 List<PortDAO.ContainerRecord> containersToSchedule,
                                 PortDAO portDAO, CountDownLatch completionLatch) {
        this.craneId = craneId;
        this.yardBlock = yardBlock;
        this.containersToSchedule = containersToSchedule;
        this.portDAO = portDAO;
        this.completionLatch = completionLatch;
    }

    @Override
    public void run() {
        String threadName = Thread.currentThread().getName();
        System.out.printf("[%s] Gantry Crane %s ACTIVATED for %s (Containers to process: %d)%n",
                threadName, craneId, yardBlock, containersToSchedule.size());

        for (PortDAO.ContainerRecord c : containersToSchedule) {
            try {
                // Simulate physical gantry crane movement cycle (trolley traverse & hoist positioning)
                Thread.sleep(120);

                // Fetch available yard slot for cargo category
                List<PortDAO.YardSlot> availableSlots = portDAO.getAvailableSlots(yardBlock, c.cargoCategory);
                String assignedSlotId = null;

                if (!availableSlots.isEmpty()) {
                    PortDAO.YardSlot slot = availableSlots.get(0);
                    assignedSlotId = slot.slotId;
                    portDAO.setSlotOccupied(assignedSlotId, true);
                } else {
                    assignedSlotId = yardBlock.replaceAll("\\s+", "-") + "-OVERFLOW-01";
                }

                Timestamp placementTimestamp = new Timestamp(System.currentTimeMillis());

                // Calculate Dwell Component Score (100% if <= free cap, decreases linearly if overdue)
                double dwellScore = 100.0;
                if (c.dwellDays > c.freeDwellCap) {
                    dwellScore = Math.max(0.0, 100.0 - ((c.dwellDays - c.freeDwellCap) * 15.0));
                }

                double performanceScore = 0.0;
                boolean sealTampered = "TAMPERED".equalsIgnoreCase(c.customsSealStatus);

                // Evaluate Category Specific Scoring
                switch (c.cargoCategory.toUpperCase()) {
                    case "REEFER":
                        // REEFER: Temp Stability = 50% | Power Connection SLA = 30% | Dwell Time = 20% | Max Off-Power Cap = 30 Mins
                        double tempStability = c.factor1 > 0 ? c.factor1 : 95.0;
                        double powerSla = c.factor2 > 0 ? c.factor2 : 92.0;

                        // Off-power cap penalty: if off-power > 30 mins, reduce power SLA by 40%
                        if (c.offPowerMinutes > 30) {
                            powerSla = Math.max(0.0, powerSla - 40.0);
                        }

                        performanceScore = (tempStability * 0.50) + (powerSla * 0.30) + (dwellScore * 0.20);
                        break;

                    case "STANDARD_DRY":
                        // STANDARD DRY: Stacking Efficiency = 40% | Dwell Time = 40% | Customs Verification = 20%
                        double stackingEff = c.factor1 > 0 ? c.factor1 : 88.0;
                        double customsVerif = c.factor2 > 0 ? c.factor2 : 90.0;
                        performanceScore = (stackingEff * 0.40) + (dwellScore * 0.40) + (customsVerif * 0.20);
                        break;

                    case "HAZMAT":
                        // HAZMAT: Isolation Safety = 60% | Inspection Score = 30% | Dwell Time = 10%
                        double isolationSafety = c.factor1 > 0 ? c.factor1 : 92.0;
                        double inspectionScore = c.factor2 > 0 ? c.factor2 : 90.0;
                        performanceScore = (isolationSafety * 0.60) + (inspectionScore * 0.30) + (dwellScore * 0.10);
                        break;

                    default:
                        performanceScore = 75.0;
                        break;
                }

                // Determine Operational Performance Band
                String band;
                String status;

                if (sealTampered || performanceScore < 60.0) {
                    band = "Seized / Audited (Container Blocked)";
                    status = "BLOCKED_AUDIT";
                } else if (performanceScore >= 90.0) {
                    band = "Port Priority Clear (Fast-Track Gate Out)";
                    status = "CLEARED";
                } else if (performanceScore >= 75.0) {
                    band = "Standard Flow (Normal Yard Clearance)";
                    status = "CLEARED";
                } else {
                    band = "Delayed Clearance (Demurrage Warning Issued)";
                    status = "CLEARED";
                }

                // Update Phase 2 Yard Storage data in database
                portDAO.updateContainerPhase2(
                        c.containerId,
                        assignedSlotId,
                        placementTimestamp,
                        c.reeferPowerKw,
                        c.offPowerMinutes,
                        c.customsSealStatus,
                        performanceScore,
                        band,
                        status
                );

                // Update local in-memory container reference
                c.slotId = assignedSlotId;
                c.gantryPlacementTime = placementTimestamp;
                c.performanceScore = performanceScore;
                c.performanceBand = band;
                c.status = status;

                System.out.printf("[%s | %s] Stacking %s -> Slot: %s | Category: %-12s | Dwell: %d days | Score: %.1f | Band: %s%n",
                        threadName, craneId, c.containerId, assignedSlotId, c.cargoCategory, c.dwellDays, performanceScore, band);

            } catch (InterruptedException e) {
                System.err.printf("[%s | %s] Crane operation interrupted for container %s%n", threadName, craneId, c.containerId);
                Thread.currentThread().interrupt();
            }
        }

        System.out.printf("[%s] Gantry Crane %s COMPLETED block operation for %s.%n", threadName, craneId, yardBlock);
        completionLatch.countDown();
    }
}
