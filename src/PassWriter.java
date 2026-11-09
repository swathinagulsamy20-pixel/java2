import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * PassWriter handles String processing, customs document parsing via indexOf/substring,
 * gate pass formatting via String.format(), and file I/O exports to .txt files.
 */
public class PassWriter {

    // Regex for ISO 6346 Container Code validation: 4 uppercase letters followed by 7 digits
    public static final String ISO_REGEX = "^[A-Z]{4}[0-9]{7}$";
    private static final Pattern ISO_PATTERN = Pattern.compile(ISO_REGEX);

    /**
     * Validates Container ISO Code using the assigned regular expression.
     * @param containerCode code string
     * @return true if valid ISO container code, false otherwise
     */
    public static boolean validateIsoCode(String containerCode) {
        if (containerCode == null) return false;
        return ISO_PATTERN.matcher(containerCode.trim()).matches();
    }

    /**
     * Parses Customs Documentation text explicitly using String indexOf() and substring()
     * as required by the technical specification.
     * 
     * Example input:
     * "DECL:[DEC-2026-901];SEAL_CODE:[SL-OK-9012];INSPECTION:[CLEARED_GREEN];CATEGORY:[REEFER];ORIGIN:[SGSIN]"
     */
    public static Map<String, String> parseCustomsDoc(String docText) {
        Map<String, String> resultMap = new HashMap<>();
        if (docText == null || docText.trim().isEmpty()) {
            return resultMap;
        }

        int currentIndex = 0;
        int len = docText.length();

        while (currentIndex < len) {
            int keyEndIndex = docText.indexOf(":[", currentIndex);
            if (keyEndIndex == -1) {
                break;
            }

            // Extract key using substring
            String key = docText.substring(currentIndex, keyEndIndex).trim();

            int valueStartIndex = keyEndIndex + 2; // skip ":["
            int valueEndIndex = docText.indexOf("]", valueStartIndex);
            if (valueEndIndex == -1) {
                break;
            }

            // Extract value using substring
            String value = docText.substring(valueStartIndex, valueEndIndex).trim();
            resultMap.put(key, value);

            // Move to next entry after ';'
            int nextDelim = docText.indexOf(";", valueEndIndex);
            if (nextDelim == -1) {
                break;
            }
            currentIndex = nextDelim + 1;
        }

        return resultMap;
    }

    /**
     * Calculates Demurrage Fine based on the assignment mathematical formula:
     * TotalDemurrageFine = (ActualDwellDays - FreeDaysCap) * DailyDemurrageRate
     */
    public static double calculateDemurrageFine(int actualDwellDays, int freeDaysCap, double dailyDemurrageRate) {
        if (actualDwellDays > freeDaysCap) {
            return (actualDwellDays - freeDaysCap) * dailyDemurrageRate;
        }
        return 0.0;
    }

    /**
     * Formats an individual gate pass using String.format() with structured borders and fields.
     */
    public static String formatGatePass(PortDAO.GatePassRecord pass, PortDAO.ContainerRecord container) {
        StringBuilder sb = new StringBuilder();
        String line = "+-------------------------------------------------------------------------+\n";

        sb.append(line);
        sb.append(String.format("|                   INTERNATIONAL PORT CONTAINER TERMINAL                 |\n"));
        sb.append(String.format("|                      OFFICIAL GATE DISPATCH PASS                        |\n"));
        sb.append(line);
        sb.append(String.format("| Pass Identifier       : %-47s |\n", pass.passId));
        sb.append(String.format("| Gate Out Timestamp    : %-47s |\n", pass.gateOutTime != null ? pass.gateOutTime.toString() : "N/A"));
        sb.append(String.format("| Issuing Authority     : %-47s |\n", pass.issuedBy));
        sb.append(line);
        sb.append(String.format("| Container ISO Code    : %-47s |\n", pass.containerId));
        sb.append(String.format("| Cargo Category        : %-47s |\n", container != null ? container.cargoCategory : "N/A"));
        sb.append(String.format("| Gross Weight          : %-47.2f Tons |\n", container != null ? container.weightTons : 0.0));
        sb.append(String.format("| Yard Storage Slot     : %-47s |\n", container != null && container.slotId != null ? container.slotId : "DISPATCH_BAY"));
        sb.append(line);
        sb.append(String.format("| Haulier Truck Reg No  : %-47s |\n", pass.truckRegNumber));
        sb.append(String.format("| Driver License No     : %-47s |\n", pass.driverLicense));
        sb.append(String.format("| Customs Release Stamp : %-47s |\n", pass.customsClearanceStamp));
        sb.append(line);
        sb.append(String.format("| Actual Dwell Duration : %-2d Days (Free Cap: %d Days)                       |\n", pass.actualDwellDays, pass.freeDwellCap));
        sb.append(String.format("| Daily Demurrage Rate  : $%-46.2f |\n", pass.dailyDemurrageRate));
        sb.append(String.format("| Demurrage Penalty Due : $%-46.2f |\n", pass.demurragePenalty));
        sb.append(String.format("| Performance Score     : %-5.2f / 100.0                                    |\n", pass.performanceScore));
        sb.append(String.format("| Operational Band      : %-47s |\n", pass.operationalBand));
        sb.append(String.format("| Gate Clearance Status : %-47s |\n", pass.demurragePenalty > 0 ? "PAID & CLEARED FOR EXIT" : "FAST-TRACK EXIT APPROVED"));
        sb.append(line);
        sb.append(String.format("| Remarks               : %-47s |\n", pass.remarks != null ? pass.remarks : "STANDARD DISPATCH"));
        sb.append(line);
        sb.append("\n");

        return sb.toString();
    }

    /**
     * Exports all gate-out clearance passes to the specified text file.
     */
    public static boolean exportGatePassesToFile(List<PortDAO.GatePassRecord> passes,
                                                 Map<String, PortDAO.ContainerRecord> containerMap,
                                                 String filePath) {
        File file = new File(filePath);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("=================================================================================\n");
            writer.write("                 DAILY GATE-OUT CLEARANCE PASSES EXPORT LOG                      \n");
            writer.write("=================================================================================\n\n");

            if (passes.isEmpty()) {
                writer.write("No gate passes issued yet.\n");
            } else {
                for (PortDAO.GatePassRecord pass : passes) {
                    PortDAO.ContainerRecord container = containerMap.get(pass.containerId);
                    writer.write(formatGatePass(pass, container));
                }
            }
            return true;
        } catch (IOException e) {
            System.err.println("[PassWriter] Failed to export gate passes: " + e.getMessage());
            return false;
        }
    }

    /**
     * Exports itemized Demurrage Billing Logs to the specified text file.
     */
    public static boolean exportDemurrageBillingToFile(List<PortDAO.ContainerRecord> allContainers,
                                                       double terminalDwellIndex,
                                                       String filePath) {
        File file = new File(filePath);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("==============================================================================================\n");
            writer.write("              INTERNATIONAL PORT TERMINAL - DEMURRAGE BILLING & DWELL AUDIT                   \n");
            writer.write("==============================================================================================\n");
            writer.write(String.format("Terminal Container Dwell Index (TCDI): %.2f Days / Container%n", terminalDwellIndex));
            writer.write("Formula: TCDI = [SUM(IndividualContainerDwellDays)] / TotalProcessedContainers\n");
            writer.write("Demurrage Formula: TotalDemurrageFine = (ActualDwellDays - FreeDaysCap) * DailyDemurrageRate\n");
            writer.write("----------------------------------------------------------------------------------------------\n");
            writer.write(String.format("%-14s | %-13s | %-6s | %-6s | %-8s | %-12s | %-12s | %s%n",
                    "Container ID", "Category", "Dwell", "Cap", "Overdue", "Daily Rate", "Total Fine", "Billing Status"));
            writer.write("----------------------------------------------------------------------------------------------\n");

            double totalTerminalDemurrageRevenue = 0.0;
            int totalOverdueContainers = 0;

            for (PortDAO.ContainerRecord c : allContainers) {
                int overdueDays = Math.max(0, c.dwellDays - c.freeDwellCap);
                double fine = calculateDemurrageFine(c.dwellDays, c.freeDwellCap, c.dailyDemurrageRate);
                totalTerminalDemurrageRevenue += fine;

                String billingStatus;
                if (fine > 0) {
                    totalOverdueContainers++;
                    billingStatus = "DEMURRAGE_PENALTY_BILLED";
                } else {
                    billingStatus = "FREE_DWELL_PERIOD";
                }

                writer.write(String.format("%-14s | %-13s | %-6d | %-6d | %-8d | $%-11.2f | $%-11.2f | %s%n",
                        c.containerId,
                        c.cargoCategory,
                        c.dwellDays,
                        c.freeDwellCap,
                        overdueDays,
                        c.dailyDemurrageRate,
                        fine,
                        billingStatus));
            }

            writer.write("----------------------------------------------------------------------------------------------\n");
            writer.write(String.format("Total Containers Evaluated    : %d%n", allContainers.size()));
            writer.write(String.format("Total Demurrage Penalties     : %d Containers Exceeded Cap%n", totalOverdueContainers));
            writer.write(String.format("Total Demurrage Revenue Due   : $%.2f%n", totalTerminalDemurrageRevenue));
            writer.write("==============================================================================================\n");

            return true;
        } catch (IOException e) {
            System.err.println("[PassWriter] Failed to export demurrage billing: " + e.getMessage());
            return false;
        }
    }
}
