package jrds.agent.linux;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jrds.agent.Start;

public class Diskstats extends LProbeProc {
    
    static private final String[] PREFIXES = {"/dev/mapper/", "/dev/disk/", "/dev/"};
    
    private String disk;
    static private final int offset = 2;

    public Boolean configure(String disk) {
        try {
            File diskFile = null;
            for (String tryprefix: PREFIXES) {
                diskFile = new File(tryprefix + disk);
                if (diskFile.exists()) {
                    break;
                } else {
                    diskFile = null;
                }
            }
            if (diskFile == null) {
                throw new RuntimeException("Disk '" + disk + "' don't exists");
            }
            String realdisk = diskFile.getCanonicalPath();
            realdisk = realdisk.replace("/dev/", "");
            this.disk = realdisk;
            return super.configure();
        } catch (IOException e) {
            throw new RuntimeException("can't resolve path to  '" + disk + "':" + e.getMessage(), e);
        }
    }

    public Map<String, Number> parse(BufferedReader r) throws IOException {
        Map<String, Number> retValues = new HashMap<String, Number>();
        String line;
        boolean found = false;
        while(! found && ((line = r.readLine()) != null)) {
            if(line.indexOf(" " + disk + " ") >= 0) {
                found = true;
                String[] values = line.trim().split("\\s+");
                //Full line, with all the values
                if(values.length > offset + 7 ) {
                    retValues.put("rrqm", Start.parseStringNumber(values[offset + 2], Double.NaN));
                    retValues.put("wrqm", Start.parseStringNumber(values[offset + 6], Double.NaN));
                    retValues.put("r", Start.parseStringNumber(values[offset + 1], Double.NaN));
                    retValues.put("w", Start.parseStringNumber(values[offset + 5], Double.NaN));
                    retValues.put("rsec", Start.parseStringNumber(values[offset + 3], Double.NaN));
                    retValues.put("wsec", Start.parseStringNumber(values[offset + 7], Double.NaN));
                    retValues.put("rwait", Start.parseStringNumber(values[offset + 4], Double.NaN));
                    retValues.put("wwait", Start.parseStringNumber(values[offset + 8], Double.NaN));
                    retValues.put("qu-sz", Start.parseStringNumber(values[offset + 9], Double.NaN));
                    retValues.put("waittm", Start.parseStringNumber(values[offset + 10], Double.NaN));
                    retValues.put("wwaittm", Start.parseStringNumber(values[offset + 11], Double.NaN));
                }
                else {
                    retValues.put("r", Start.parseStringNumber(values[offset + 1], Double.NaN));
                    retValues.put("rsec", Start.parseStringNumber(values[offset + 2], Double.NaN));
                    retValues.put("w", Start.parseStringNumber(values[offset + 3], Double.NaN));
                    retValues.put("wsec", Start.parseStringNumber(values[offset + 4], Double.NaN));
                }
            }
        }
        return retValues;
    }

    public String getName() {
        return "ds-" + disk;
    }

}
