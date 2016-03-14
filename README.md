# compute-engine-usage-parser
A utility to parse and aggregate Google Compute Engine CSV usage files. Ensure you have Google Compute Engine usage export enabled in your cloud console as shown in this screenshot

## Usage:

```bash
    git clone https://github.com/omerio/compute-engine-usage-parser.git
    cd compute-engine-usage-parser
    mkdir dir usage
    # Download the usage files, needs cloud sdk
    gsutil -m cp gs://<usage-bucket>/*.csv usage/
    ./parse.sh usage
```  

