package jrds.agent;

import java.util.HashMap;
import java.util.Map;

public class TestProbe extends LProbe {

    public String getName() {
        return "TestProbe";
    }

    public Map<String,Number> query() {
        Map<String,Number> m = new HashMap<String,Number>(1);
        m.put("time", new Double(System.currentTimeMillis()));
        return m;
    }

}
