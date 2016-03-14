/**
 * The contents of this file may be used under the terms of the Apache License, Version 2.0
 * in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 *
 * Copyright 2014, Ecarf.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package uk.co.inetria.gce;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

/**
 * 
 * Parse Google Compute Engine Usage CSV files
 * Report Date  MeasurementId   Quantity    Unit    Resource URI    ResourceId  Location
 * @author Omer Dawelbeit (omerio)
 *
 */
public class GceUsageParser {

    private static final String USAGE_PREFIX = "usage_gce_";

    private static final String MEASURE_PREFIX = "com.google.cloud/services/compute-engine/";

    private static final String VM = "Vmimage";

    private static final String NETWORK = "Network";
    private static final String DISK = "StoragePdCapacity";
    private static final String DISK_SSD = "StoragePdSsd";
    private static final String IMAGE = "StorageImage";
    private static final String CONTAINER_ENGINE_VM = "Licensed";

    public static final int BUF_SIZE = 1024 * 1024;

    private static final int MINUTE = 60;
    private static final int HOUR = MINUTE * MINUTE;

    private Set<String> files = new HashSet<>();

    private Set<String> measurementIds = new HashSet<>();

    private Set<String> vms = new HashSet<>();

    private Map<String, Usage> usages = new TreeMap<>();

    private int numberOfVms;


    public GceUsageParser(String folder) throws Exception {

        super();

        // local file
        DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
            public boolean accept(Path file) throws IOException {

                String filename = file.toString(); 
                return filename.contains(USAGE_PREFIX);
            }
        };

        Path dir = Paths.get(folder);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, filter)) {
            for (Path path : stream) {
                this.files.add(path.toString());
            }
        }

    }

    public void parse() throws FileNotFoundException, IOException {

        for(String file: files) {

            try (BufferedReader reader = new BufferedReader(new FileReader(file), BUF_SIZE);) {

                Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader().withSkipHeaderRecord().parse(reader);

                for (CSVRecord record : records) {

                    String measurement = StringUtils.remove(record.get(1), MEASURE_PREFIX);
                    this.measurementIds.add(measurement);

                    if(measurement.contains(VM) || measurement.contains(CONTAINER_ENGINE_VM)) {
                        this.numberOfVms++;

                        this.vms.add(record.get(4));
                    }

                    Usage usage = this.usages.get(measurement);

                    if(usage == null) {
                        usage = new Usage();
                        this.usages.put(measurement, usage);
                    }

                    long value = Long.parseLong(record.get(2));

                    usage.raw += value;

                    if(measurement.contains(VM) || measurement.contains(CONTAINER_ENGINE_VM)) {

                        // hourly based billing
                        long adjusted = value;
                        if(adjusted < HOUR) {
                            adjusted = HOUR;

                        } else if(adjusted % HOUR > 0) {
                            adjusted = (long) (HOUR * Math.ceil(adjusted / (double) HOUR));
                        }
                        usage.adjusted += adjusted;

                    }

                }
            }
        }
        
        System.out.println("Unique measurements");
        for(String measureId: this.measurementIds) {
            System.out.println(measureId);
        }

        System.out.println("Total number of started VMs: " + this.numberOfVms);
        System.out.println("Total number of unique VMs: " + this.vms.size());
        
        for(String vmId: this.vms) {
            System.out.println(vmId);
        }
        
        System.out.println("Aggregated usage");
        
        System.out.println("MeasurementId,Quantity,Per-hour Quantity");
        
        for(Entry<String, Usage> entry: this.usages.entrySet()) {
            Usage usage = entry.getValue();
            System.out.println(entry.getKey() + ',' + usage.raw  + ',' + usage.adjusted );
        }
    }

    /**
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String [] args) throws Exception {
        if(args.length != 1) {
            System.out.println("Usage: UsageParser /path/to/directory");
            System.exit(-1);
        }

        String path = args[0];

        GceUsageParser parser = new GceUsageParser(path);
        parser.parse();



    }

    public class Usage {
        long raw;
        long adjusted;
    }

}
