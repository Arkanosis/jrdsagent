package jrds.agent.linux;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProcSmaps extends AbstractProcessParser {

    private static final String HEADERPATTERN = "[0-9a-f]+-[0-9a-f]+ (?<perm>....) [0-9a-f]+ (?<majorminor>..:..) \\d+ *(?<filename>.+)?";
    private static final String SIZEPATTERN = "(?<key>.*): +(?<value>\\d+) kB";
    private static final Pattern LINEPATTERN = Pattern.compile(String.format("^(?:%s)|(?:%s)$", HEADERPATTERN, SIZEPATTERN));
    private static final Set<String> IGNORE = new HashSet<String>();
    static {
        IGNORE.addAll(Arrays.asList("KernelPageSize", "MMUPageSize"));
    }

    @Override
    protected Map<String, Number> parseProc(int pid) {
        BufferedReader r = null;
        try {
            Map<String, Map<String, Long>> areadetails = new HashMap<String, Map<String, Long>>();
            File smaps = new File("/proc/" + pid + "/smaps");
            r = new BufferedReader(new InputStreamReader(new FileInputStream(smaps), LINUXFSCHARSET));
            String line;
            Map<String, Long> currentareaddetails = null;
            while ((line = r.readLine()) != null) {
                Matcher match = LINEPATTERN.matcher(line);
                if ( !match.matches()) {
                    continue;
                }
                String majorminor = match.group("majorminor");
                String key = match.group("key");
                if (majorminor != null) {
                    String areaname = majorminor;
                    String filename = match.group("filename");
                    if ("00:00".equals(majorminor)) {
                        areaname = "anonymous";
                    } else if (filename != null) {
                        areaname = "mappedfiles";
                    }
                    if (! areadetails.containsKey(areaname)) {
                        areadetails.put(areaname, new HashMap<String, Long>(14));
                    }
                    currentareaddetails = areadetails.get(areaname);
                } else if (currentareaddetails == null) {
                    continue;
                } else if (key != null && ! IGNORE.contains(key)) {
                    long value = Long.parseLong(match.group("value")) * 1024;
                    if (currentareaddetails.containsKey(key)) {
                        long oldvalue = currentareaddetails.get(key);
                        currentareaddetails.put(key, oldvalue + value);
                    } else {
                        currentareaddetails.put(key, value);
                    }
                }
            }
            Map<String, Number> collected = new HashMap<String, Number>();
            for(Map.Entry<String, Map<String, Long>> i: areadetails.entrySet()) {
                for (Map.Entry<String, Long> j: i.getValue().entrySet()) {
                    collected.put(String.format("%s:%s", i.getKey(), j.getKey()), j.getValue());
                }
            }
            return collected;
        } catch (FileNotFoundException e) {
            return Collections.emptyMap();
        } catch (IOException e) {
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException e) {
                }
            }
        }
        return Collections.emptyMap();
    }

    @Override
    protected long getProcUptime(Map<String, Number> values) {
        return Long.MAX_VALUE;
    }

    @Override
    public String getName() {
        return "pismaps-" + cmdFilter.toString();
    }

    @Override
    protected long computeUpTime(long startTime) {
        return Long.MAX_VALUE;
    }

}
