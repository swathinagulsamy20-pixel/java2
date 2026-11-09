import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * PortDAO manages all relational persistence operations for
 * vessels, yard slots, containers, and gate passes.
 */
public class PortDAO {

    // =========================================================================
    // Domain Models
    // =========================================================================

    public static class VesselRecord {
        public String voyageId;
        public String vesselName;
        public String originPort;
        public String destinationPort;
        public String berthNumber;
        public Timestamp arrivalDate;
        public String status;

        public VesselRecord(String voyageId, String vesselName, String originPort,
                            String destinationPort, String berthNumber, Timestamp arrivalDate, String status) {
            this.voyageId = voyageId;
            this.vesselName = vesselName;
            this.originPort = originPort;
            this.destinationPort = destinationPort;
            this.berthNumber = berthNumber;
            this.arrivalDate = arrivalDate;
            this.status = status;
        }
    }

    public static class YardSlot {
        public String slotId;
        public String blockName;
        public int tierLevel;
        public String slotType;
        public boolean isOccupied;
        public String assignedCraneId;

        public YardSlot(String slotId, String blockName, int tierLevel, String slotType, boolean isOccupied, String assignedCraneId) {
            this.slotId = slotId;
            this.blockName = blockName;
            this.tierLevel = tierLevel;
            this.slotType = slotType;
            this.isOccupied = isOccupied;
            this.assignedCraneId = assignedCraneId;
        }
    }

    public static class ContainerRecord {
        public String containerId;
        public String voyageId;
        public String cargoCategory;
        public double weightTons;
        public String slotId;
        public String yardBlock;
        public String customsDocText;
        public String customsSealStatus;
        public Timestamp gantryPlacementTime;
        public double reeferPowerKw;
        public int offPowerMinutes;
        public int dwellDays;
        public int freeDwellCap;
        public double dailyDemurrageRate;
        public double performanceScore;
        public String performanceBand;
        public String status;
        public double factor1; // Temp Stability / Stacking Efficiency / Isolation Safety
        public double factor2; // Power SLA / Customs Verification / Inspection Score

        public ContainerRecord() {}

        public ContainerRecord(String containerId, String voyageId, String cargoCategory, double weightTons,
                               String slotId, String yardBlock, String customsDocText, String customsSealStatus,
                               Timestamp gantryPlacementTime, double reeferPowerKw, int offPowerMinutes,
                               int dwellDays, int freeDwellCap, double dailyDemurrageRate,
                               double performanceScore, String performanceBand, String status) {
            this.containerId = containerId;
            this.voyageId = voyageId;
            this.cargoCategory = cargoCategory;
            this.weightTons = weightTons;
            this.slotId = slotId;
            this.yardBlock = yardBlock;
            this.customsDocText = customsDocText;
            this.customsSealStatus = customsSealStatus;
            this.gantryPlacementTime = gantryPlacementTime;
            this.reeferPowerKw = reeferPowerKw;
            this.offPowerMinutes = offPowerMinutes;
            this.dwellDays = dwellDays;
            this.freeDwellCap = freeDwellCap;
            this.dailyDemurrageRate = dailyDemurrageRate;
            this.performanceScore = performanceScore;
            this.performanceBand = performanceBand;
            this.status = status;
        }
    }

    public static class GatePassRecord {
        public String passId;
        public String containerId;
        public String truckRegNumber;
        public String driverLicense;
        public String customsClearanceStamp;
        public int actualDwellDays;
        public int freeDwellCap;
        public double dailyDemurrageRate;
        public double demurragePenalty;
        public double performanceScore;
        public String operationalBand;
        public Timestamp gateOutTime;
        public String issuedBy;
        public String remarks;

        public GatePassRecord(String passId, String containerId, String truckRegNumber, String driverLicense,
                              String customsClearanceStamp, int actualDwellDays, int freeDwellCap,
                              double dailyDemurrageRate, double demurragePenalty, double performanceScore,
                              String operationalBand, Timestamp gateOutTime, String issuedBy, String remarks) {
            this.passId = passId;
            this.containerId = containerId;
            this.truckRegNumber = truckRegNumber;
            this.driverLicense = driverLicense;
            this.customsClearanceStamp = customsClearanceStamp;
            this.actualDwellDays = actualDwellDays;
            this.freeDwellCap = freeDwellCap;
            this.dailyDemurrageRate = dailyDemurrageRate;
            this.demurragePenalty = demurragePenalty;
            this.performanceScore = performanceScore;
            this.operationalBand = operationalBand;
            this.gateOutTime = gateOutTime;
            this.issuedBy = issuedBy;
            this.remarks = remarks;
        }
    }

    // =========================================================================
    // DAO Operations
    // =========================================================================

    /**
     * Inserts or refreshes a vessel voyage schedule.
     */
    public boolean upsertVessel(VesselRecord v) {
        String sql = "INSERT INTO vessels (voyage_id, vessel_name, origin_port, destination_port, berth_number, arrival_date, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE vessel_name=VALUES(vessel_name), berth_number=VALUES(berth_number), status=VALUES(status)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, v.voyageId);
            ps.setString(2, v.vesselName);
            ps.setString(3, v.originPort);
            ps.setString(4, v.destinationPort);
            ps.setString(5, v.berthNumber);
            ps.setTimestamp(6, v.arrivalDate);
            ps.setString(7, v.status);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[PortDAO] Error upserting vessel: " + e.getMessage());
            return false;
        }
    }

    /**
     * Phase 1 (Vessel Unloading): Record Container ISO Code, Vessel Voyage ID, Weight (Tons), Yard Block Assignment.
     */
    public boolean registerContainerPhase1(ContainerRecord c) {
        String sql = "INSERT INTO containers (container_id, voyage_id, cargo_category, weight_tons, yard_block, " +
                     "customs_doc_text, customs_seal_status, dwell_days, free_dwell_cap, daily_demurrage_rate, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE cargo_category=VALUES(cargo_category), weight_tons=VALUES(weight_tons), " +
                     "yard_block=VALUES(yard_block), customs_doc_text=VALUES(customs_doc_text), " +
                     "customs_seal_status=VALUES(customs_seal_status), dwell_days=VALUES(dwell_days), " +
                     "free_dwell_cap=VALUES(free_dwell_cap), daily_demurrage_rate=VALUES(daily_demurrage_rate)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.containerId);
            ps.setString(2, c.voyageId);
            ps.setString(3, c.cargoCategory);
            ps.setDouble(4, c.weightTons);
            ps.setString(5, c.yardBlock);
            ps.setString(6, c.customsDocText);
            ps.setString(7, c.customsSealStatus);
            ps.setInt(8, c.dwellDays);
            ps.setInt(9, c.freeDwellCap);
            ps.setDouble(10, c.dailyDemurrageRate);
            ps.setString(11, "DISCHARGED");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[PortDAO] Error in Phase 1 container registration: " + e.getMessage());
            return false;
        }
    }

    /**
     * Phase 2 (Yard Storage): Record Gantry Crane Placement Timestamp, Reefer Power Draw Logs, and Customs Seal Status.
     */
    public boolean updateContainerPhase2(String containerId, String slotId, Timestamp placementTime,
                                         double reeferKw, int offPowerMin, String sealStatus,
                                         double perfScore, String perfBand, String status) {
        String sql = "UPDATE containers SET slot_id=?, gantry_placement_time=?, reefer_power_kw=?, " +
                     "off_power_minutes=?, customs_seal_status=?, performance_score=?, performance_band=?, " +
                     "status=? WHERE container_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, slotId);
            ps.setTimestamp(2, placementTime);
            ps.setDouble(3, reeferKw);
            ps.setInt(4, offPowerMin);
            ps.setString(5, sealStatus);
            ps.setDouble(6, perfScore);
            ps.setString(7, perfBand);
            ps.setString(8, status);
            ps.setString(9, containerId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[PortDAO] Error in Phase 2 container update: " + e.getMessage());
            return false;
        }
    }

    /**
     * Phase 3 (Gate Dispatch): Record Outbound Truck Registration, Customs Release Clearance Stamp, and Gate-Out Time.
     */
    public boolean recordGateDispatchPhase3(GatePassRecord pass) {
        String insertPassSql = "INSERT INTO gate_passes (pass_id, container_id, truck_reg_number, driver_license, " +
                               "customs_clearance_stamp, actual_dwell_days, free_dwell_cap, daily_demurrage_rate, " +
                               "demurrage_penalty, performance_score, operational_band, gate_out_time, issued_by, remarks) " +
                               "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                               "ON DUPLICATE KEY UPDATE gate_out_time=VALUES(gate_out_time), remarks=VALUES(remarks)";
        String updateContainerSql = "UPDATE containers SET status = 'GATE_OUT' WHERE container_id = ?";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement psPass = conn.prepareStatement(insertPassSql)) {
                psPass.setString(1, pass.passId);
                psPass.setString(2, pass.containerId);
                psPass.setString(3, pass.truckRegNumber);
                psPass.setString(4, pass.driverLicense);
                psPass.setString(5, pass.customsClearanceStamp);
                psPass.setInt(6, pass.actualDwellDays);
                psPass.setInt(7, pass.freeDwellCap);
                psPass.setDouble(8, pass.dailyDemurrageRate);
                psPass.setDouble(9, pass.demurragePenalty);
                psPass.setDouble(10, pass.performanceScore);
                psPass.setString(11, pass.operationalBand);
                psPass.setTimestamp(12, pass.gateOutTime);
                psPass.setString(13, pass.issuedBy);
                psPass.setString(14, pass.remarks);
                psPass.executeUpdate();
            }

            try (PreparedStatement psCont = conn.prepareStatement(updateContainerSql)) {
                psCont.setString(1, pass.containerId);
                psCont.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("[PortDAO] Error recording Phase 3 gate dispatch: " + e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { /* ignored */ }
            }
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { /* ignored */ }
            }
        }
    }

    /**
     * Fetches available unoccupied yard slots compatible with the block and cargo type.
     */
    public synchronized List<YardSlot> getAvailableSlots(String blockName, String slotType) {
        List<YardSlot> list = new ArrayList<>();
        String sql = "SELECT slot_id, block_name, tier_level, slot_type, is_occupied, assigned_crane_id " +
                     "FROM yard_slots WHERE is_occupied = FALSE AND slot_type = ? AND block_name LIKE ? " +
                     "ORDER BY tier_level ASC, slot_id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, slotType);
            ps.setString(2, "%" + blockName + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new YardSlot(
                        rs.getString("slot_id"),
                        rs.getString("block_name"),
                        rs.getInt("tier_level"),
                        rs.getString("slot_type"),
                        rs.getBoolean("is_occupied"),
                        rs.getString("assigned_crane_id")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[PortDAO] Error fetching available yard slots: " + e.getMessage());
        }
        return list;
    }

    /**
     * Marks yard slot as occupied.
     */
    public synchronized boolean setSlotOccupied(String slotId, boolean occupied) {
        String sql = "UPDATE yard_slots SET is_occupied = ? WHERE slot_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, occupied);
            ps.setString(2, slotId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[PortDAO] Error updating slot status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves all containers currently registered.
     */
    public List<ContainerRecord> getAllContainers() {
        List<ContainerRecord> list = new ArrayList<>();
        String sql = "SELECT container_id, voyage_id, cargo_category, weight_tons, slot_id, yard_block, " +
                     "customs_doc_text, customs_seal_status, gantry_placement_time, reefer_power_kw, " +
                     "off_power_minutes, dwell_days, free_dwell_cap, daily_demurrage_rate, performance_score, " +
                     "performance_band, status FROM containers ORDER BY container_id ASC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new ContainerRecord(
                    rs.getString("container_id"),
                    rs.getString("voyage_id"),
                    rs.getString("cargo_category"),
                    rs.getDouble("weight_tons"),
                    rs.getString("slot_id"),
                    rs.getString("yard_block"),
                    rs.getString("customs_doc_text"),
                    rs.getString("customs_seal_status"),
                    rs.getTimestamp("gantry_placement_time"),
                    rs.getDouble("reefer_power_kw"),
                    rs.getInt("off_power_minutes"),
                    rs.getInt("dwell_days"),
                    rs.getInt("free_dwell_cap"),
                    rs.getDouble("daily_demurrage_rate"),
                    rs.getDouble("performance_score"),
                    rs.getString("performance_band"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[PortDAO] Error fetching containers: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves all issued gate passes.
     */
    public List<GatePassRecord> getAllGatePasses() {
        List<GatePassRecord> list = new ArrayList<>();
        String sql = "SELECT pass_id, container_id, truck_reg_number, driver_license, customs_clearance_stamp, " +
                     "actual_dwell_days, free_dwell_cap, daily_demurrage_rate, demurrage_penalty, performance_score, " +
                     "operational_band, gate_out_time, issued_by, remarks FROM gate_passes ORDER BY gate_out_time ASC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new GatePassRecord(
                    rs.getString("pass_id"),
                    rs.getString("container_id"),
                    rs.getString("truck_reg_number"),
                    rs.getString("driver_license"),
                    rs.getString("customs_clearance_stamp"),
                    rs.getInt("actual_dwell_days"),
                    rs.getInt("free_dwell_cap"),
                    rs.getDouble("daily_demurrage_rate"),
                    rs.getDouble("demurrage_penalty"),
                    rs.getDouble("performance_score"),
                    rs.getString("operational_band"),
                    rs.getTimestamp("gate_out_time"),
                    rs.getString("issued_by"),
                    rs.getString("remarks")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[PortDAO] Error fetching gate passes: " + e.getMessage());
        }
        return list;
    }

    /**
     * Calculates Terminal Container Dwell Index (TCDI):
     * Formula: TCDI = (Sum IndividualContainerDwellDays) / TotalProcessedContainers
     */
    public double calculateTerminalContainerDwellIndex() {
        String sql = "SELECT SUM(dwell_days) AS total_dwell, COUNT(*) AS total_count FROM containers";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int totalDwell = rs.getInt("total_dwell");
                int totalCount = rs.getInt("total_count");
                if (totalCount > 0) {
                    return (double) totalDwell / totalCount;
                }
            }
        } catch (SQLException e) {
            System.err.println("[PortDAO] Error computing TCDI: " + e.getMessage());
        }
        return 0.0;
    }

    /**
     * Resets container runtime states and releases yard slots for clean demo execution.
     */
    public void resetOperationalState() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM gate_passes");
            stmt.executeUpdate("DELETE FROM containers");
            stmt.executeUpdate("UPDATE yard_slots SET is_occupied = FALSE");
        } catch (SQLException e) {
            System.err.println("[PortDAO] Error resetting operational state: " + e.getMessage());
        }
    }
}
