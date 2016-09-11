package in.dhali.bumperjumper.bumperjumper;

import java.util.ArrayList;

/**
 * Created by mithun on 11-09-2016.
 */
public class Bump {
    public long ms;
    public double lat;
    public double lng;
    public double energy;

    public Bump(long ms, double lat, double lng, double energy) {
        this.ms = ms;
        this.lat = lat;
        this.lng = lng;
        this.energy = energy;
    }

    public static ArrayList<Bump> fromLines(String s) {
        String lines[] = s.trim().split("\\r?\\n");
        ArrayList<Bump> bumps = new ArrayList<>(lines.length);

        for (String line : lines) {
            if (line.isEmpty()) {
                continue;
            }

            if (line.charAt(0) == '#') {
                continue;
            }

            String[] words = line.split(",");
            Bump bump = new Bump(
                    Long.parseLong(words[0]),
                    Double.parseDouble(words[1]), Double.parseDouble(words[2]),
                    Double.parseDouble(words[3])
            );

            bumps.add(bump);
        }

        return bumps;
    }
}
